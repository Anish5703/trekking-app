package com.example.trekking_app.service.contour;
import com.example.trekking_app.dto.contour.ContourResponse;
import com.example.trekking_app.dto.contour.PointData;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.exception.contour.ElevationUnavailableException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.repository.RouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ElevationProfileService {
    private static final Logger log = LoggerFactory.getLogger(ElevationProfileService.class);

    private final RouteRepository routeRepo;
    private final TrackPointStore store;
    private final ElevationPopulateService populateService;   // separate bean so @CacheEvict goes through the proxy
    private final double noiseThreshold;

    ElevationProfileService(RouteRepository routeRepo,
                            TrackPointStore store,
                            ElevationPopulateService populateService,
                            @Value("${trekking.contour.noise-threshold-m:3.0}") double noiseThreshold) {
        this.routeRepo = routeRepo;
        this.store = store;
        this.populateService = populateService;
        this.noiseThreshold = noiseThreshold;
    }

    /** Cached per route. Incomplete responses (elevation service down) are not cached. */
   @Cacheable(cacheNames = "route-contour", key = "#routeId", unless = "!#result.data.elevationComplete")
    public ApiResponse<ContourResponse> getContour(int routeId) {
        Route route = routeRepo.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("route", "id", routeId));

        List<PointData> pts = store.load(routeId);

        // populate only if some trackpoints have no elevation
        if (pts.stream().anyMatch(PointData::needsElevation)) {
            try {
                populateService.populate(routeId);
                pts = store.load(routeId);                         // reload with saved elevations
            } catch (ElevationUnavailableException ex) {
                log.warn("Elevation populate failed for route {}: {}", routeId, ex.getMessage());
                // continue with what we have; response is flagged elevationComplete=false
            }
        }

        List<Double> ele = new ArrayList<>(pts.size());
        List<ContourResponse.Point> out = new ArrayList<>(pts.size());
        boolean complete = true;
        for (PointData p : pts) {
            Double e = p.hasElevation() ? p.getElevation() : null;
            if (e == null) complete = false;
            ele.add(e);
            out.add(ContourResponse.Point.builder()
                    .lat(p.getLat())
                    .lon(p.getLon())
                    .elevation(e)
                    .build());
        }

        ContourResponse response =  ContourResponse.builder()
                .routeId(routeId)
                .pointCount(pts.size())
                .elevationComplete(complete)
                .stats(ElevationStatsCalculator.compute(ele, noiseThreshold))
                .points(out)
                .build();

        return new ApiResponse<>(response,"contour data fetched",200);
    }
}
