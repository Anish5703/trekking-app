package com.example.trekking_app.controller;
import com.example.trekking_app.dto.geoJson.GeoJsonFeatureCollection;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.*;
import com.example.trekking_app.service.route.RouteService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/route")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService)
    {
        this.routeService = routeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RouteDetails>>> handleGetAllDestinationRoutes(@NonNull @RequestParam Integer destinationId)
    {
        ApiResponse<List<RouteDetails>> response = routeService.getAllDestinationRoutes(destinationId);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<ApiResponse<RouteResponse>> handleGetRoute(@NonNull @PathVariable Integer routeId)
    {
        ApiResponse<RouteResponse> response = routeService.getRoute(routeId);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/{routeId}/geoJson")
    public ResponseEntity<GeoJsonFeatureCollection> handleGetRouteGeoJson(@NonNull @PathVariable Integer routeId,
                                                                       @RequestParam (defaultValue = "0.00001")Double tolerance)
    {
        Instant startTime = Instant.now();
        GeoJsonFeatureCollection response = routeService.getRouteGeoJson(routeId,tolerance);
        log.info("Fetched route geJson in {} ms", Duration.between(startTime,Instant.now()).toMillis());
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<RouteDetails>>> handleGetAllRoutes(@NonNull Integer page ,@NonNull Integer size)
    {
        ApiResponse<Page<RouteDetails>> response = routeService.getAllRoutes(page,size);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<NearbyRouteResponse>>> handleGetNearbyRoutes(@RequestParam Double longitude ,
                                                                                        @RequestParam Double latitude,
                                                                                        @RequestParam Double radiusMeters,
                                                                                        @RequestParam Integer limit)
    {
        NearbyRouteRequest request = NearbyRouteRequest.builder().
                longitude(longitude).latitude(latitude).radiusMeters(radiusMeters).limit(limit).
                build();
        ApiResponse<List<NearbyRouteResponse>> response = routeService.getNearbyRoutes(request);
        return ResponseEntity.status(200).body(response);
    }
}
