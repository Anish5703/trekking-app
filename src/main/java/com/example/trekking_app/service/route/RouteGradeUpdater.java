package com.example.trekking_app.service.route;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.grade.*;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrailSegment;
import com.example.trekking_app.exception.route.InsufficientRouteDataException;
import com.example.trekking_app.model.TrailType;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteGradeUpdater {

    private final RouteGradeService gradeService;
    private final ElevationGainCalculator gainCalc;
    private final TrackPointRepository trackPointRepo;
    private final RouteRepository routeRepo;

    // ---------- API-facing: return ApiResponse like the rest of your services ----------

    public ApiResponse<GradeResult> preview(RouteInput input) {
        return new ApiResponse<>(gradeService.grade(input), "Grade preview generated", 200);
    }

    @Transactional(readOnly = true)
    public ApiResponse<GradeResult> getGrade(Integer routeId) {
        GradeResult g = find(routeId).getDifficultyResult();
        if (g == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Route " + routeId + " has not been graded yet");
        return new ApiResponse<>(g, "Route grade fetched successfully", 200);
    }

    /** Throws InsufficientRouteDataException (422) if inputs are missing. */
    @Transactional
    public ApiResponse<GradeResult> regradeRoute(Integer routeId) {
        Route r = findUnlocked(routeId);
        refreshGain(r);
        List<String> missing = missingInputs(r);
        if (!missing.isEmpty()) throw new InsufficientRouteDataException(r.getId(), missing);
        return new ApiResponse<>(apply(r), "Route graded successfully", 200);
    }

    /** Always saves terrain; grades only if the other inputs exist. */
    @Transactional
    public ApiResponse<TerrainUpdateResponse> updateTerrain(Integer routeId, TerrainProfile terrain) {
        Route r = findUnlocked(routeId);
        r.setTerrain(terrain);
        refreshGain(r);
        List<String> missing = missingInputs(r);
        GradeResult g = missing.isEmpty() ? apply(r) : null;
        String msg = missing.isEmpty()
                ? "Terrain saved and route regraded"
                : "Terrain saved. Grading skipped because inputs are missing";
        return new ApiResponse<>(new TerrainUpdateResponse(terrain, g, missing), msg, 200);
    }

    // ---------- Internal callers (GPX import, track-point edit, route update, backfill) ----------

    /** Hook method: never throws for missing data, keeps the old difficultyLevel in that case. */
    @Transactional
    public Optional<GradeResult> regrade(Route r) {
        refreshGain(r);
        if (!missingInputs(r).isEmpty()) return Optional.empty();
        return Optional.of(apply(r));
    }

    @Transactional
    public Optional<GradeResult> regrade(Integer routeId) {
        return regrade(find(routeId));
    }

    // ---------- Helpers ----------

    private Route find(Integer id) {
        return routeRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Route " + id + " not found"));
    }

    private Route findUnlocked(Integer id) {
        Route r = find(id);
        if (Boolean.TRUE.equals(r.getIsLocked()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Route " + id + " is locked");
        return r;
    }

    private void refreshGain(Route r) {
        List<Double> elev = trackPointRepo.findActiveElevations(r.getId());
        if (!elev.isEmpty()) r.setElevationGainM(gainCalc.gain(elev));
    }

    private List<String> missingInputs(Route r) {
        List<String> missing = new ArrayList<>();
        if (r.getDistanceInKm() == null || r.getDistanceInKm() <= 0) missing.add("distance_in_km");
        if (r.getMaxElevation() == null || r.getMaxElevation() <= 0) missing.add("max_elevation");
        if (r.getElevationGainM() == null) missing.add("elevation_gain_m (no track point elevations)");
        return missing;
    }

    @Transactional
    private GradeResult apply(Route r) {
        GradeResult g = gradeService.grade(buildInput(r));
        r.setDifficultyLevel(g.grade());
        r.setDifficultyResult(g);
        r.setCategory(g.category());
        routeRepo.save(r);
        return g;
    }

    private RouteInput buildInput(Route r) {
        TerrainProfile t = r.getTerrain() != null ? r.getTerrain() : new TerrainProfile();

        Set<TrailType> types = r.getTrailSegments().stream()
                .map(TrailSegment::getType)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TrailType.class)));

        boolean glacier = t.isGlacierOrMoraine() || types.contains(TrailType.ICE_TRAIL);
        boolean rough   = t.isRoughOrNoTrail()   || types.contains(TrailType.ROCKY_TRAIL);

        return new RouteInput(r.getName(), r.getDistanceInKm(), r.getElevationGainM(),
                r.getMaxElevation(), t.getHighPassM(), t.isNarrowOrExposed(), t.isLandslideOrRockfall(),
                t.isScrambling(), glacier, rough, t.isCampingRequired(),
                t.isTechnicalGear(), t.isPeakClimb(), t.getPeakAlpineGrade());
    }
}