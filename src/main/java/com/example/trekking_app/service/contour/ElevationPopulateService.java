package com.example.trekking_app.service.contour;
import com.example.trekking_app.dto.contour.LatLon;
import com.example.trekking_app.dto.contour.PointData;
import com.example.trekking_app.dto.contour.PopulateElevationResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.exception.contour.ElevationUnavailableException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.resource.ResourceUpdateFailedException;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.*;
@Slf4j
@Service
public class ElevationPopulateService {

    /** Exact coordinates (no rounding): identical lat/lon pairs are looked up once. */
    private record CoordKey(double lat, double lon) {}

    private final RouteRepository routeRepo;
    private final TrackPointStore store;
    private final ElevationClient elevationClient;
    private final TrackPointRepository tpRepo;

    ElevationPopulateService(RouteRepository routeRepo, TrackPointStore store, ElevationClient elevationClient,TrackPointRepository tpRepo) {
        this.routeRepo = routeRepo;
        this.store = store;
        this.elevationClient = elevationClient;
        this.tpRepo = tpRepo;
    }

    /**
     * Looks up elevations for all trackpoints that have none (batched), saves each result on its
     * own trackpoint, and evicts the cached route data when anything changed.
     * Throws ElevationUnavailableException (HTTP 502) if the elevation service fails.
     */
/*   @CacheEvict(cacheNames = {"route-geoJson", "route-contour"}, key = "#routeId",
           condition = "#result.elevationsFetched > 0")
 */
    public ApiResponse<PopulateElevationResponse> populate(int routeId) {
        Route route = routeRepo.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("route", "id", routeId));

        // 1) read (short tx)
        List<PointData> pts = store.load(routeId);

        // 2) trackpoints without elevation, grouped by exact coordinate
        int missingBefore = 0;
        Map<CoordKey, List<PointData>> toLookup = new LinkedHashMap<>();
        for (PointData p : pts) {
            if (p.hasElevation()) continue;
            missingBefore++;
            if (p.hasValidCoordinates()) {
                toLookup.computeIfAbsent(new CoordKey(p.getLat(), p.getLon()), k -> new ArrayList<>()).add(p);
            }
        }

        int fetched = 0;
        if (!toLookup.isEmpty()) {
            List<CoordKey> keys = new ArrayList<>(toLookup.keySet());

            // 3) batched lookup (ElevationClient chunks internally), outside any transaction
            List<Double> result;
            try {
                result = elevationClient.elevations(keys.stream().map(k -> new LatLon(k.lat(), k.lon())).toList());
            } catch (RestClientException | IllegalStateException ex) {
                log.error("Elevation Service Unavailable : {} ",ex.getMessage());
                throw new ElevationUnavailableException("Elevation service unavailable", ex);
            }

            // 4) results are in request order: map back to every trackpoint with that coordinate
            Map<Integer, Double> toSave = new HashMap<>();
            for (int i = 0; i < keys.size(); i++) {
                Double e = result.get(i);
                if (e == null) continue;                              // no DEM data at this point
                for (PointData p : toLookup.get(keys.get(i))) {
                    toSave.put(p.getId(), e);
                }
            }

            // 5) persist (short tx, JDBC batch)
            store.saveElevations(toSave);
            fetched = toSave.size();
        }
        updateRouteElevationBounds(routeId);
        PopulateElevationResponse response = PopulateElevationResponse.builder()
                .routeId(routeId)
                .pointCount(pts.size())
                .missingBefore(missingBefore)
                .elevationsFetched(fetched)
                .stillMissing(missingBefore - fetched)
                .build();
        return new ApiResponse<>(response,"elevation populated",200);
    }

    @Transactional
    void updateRouteElevationBounds(Integer routeId) {
        Route route = routeRepo.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("route", "id", routeId));
        Optional<Double> min = tpRepo.findMinElevation(routeId);
        Optional<Double> max = tpRepo.findMaxElevation(routeId);
        if (min.isEmpty() || max.isEmpty()) return;          // no elevation data: leave the route unchanged
        route.setMinElevation(min.get());
        route.setMaxElevation(max.get());
        routeRepo.save(route);
    }
}