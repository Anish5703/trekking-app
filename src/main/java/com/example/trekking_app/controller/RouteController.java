package com.example.trekking_app.controller;
import com.example.trekking_app.dto.geoJson.GeoJsonFeatureCollection;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.*;
import com.example.trekking_app.model.UserPrincipal;
import com.example.trekking_app.service.route.RouteService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<ApiResponse<RouteResponse>> handleGetRoute(@NonNull @PathVariable Integer routeId
            ,@AuthenticationPrincipal UserPrincipal user)
    {

        ApiResponse<RouteResponse> response = routeService.getRoute(routeId,user.getId());
        routeService.updateRecentlyViewedStatus(user.getId(),routeId);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @GetMapping("/{routeId}/geoJson")
    public ResponseEntity<GeoJsonFeatureCollection> handleGetRouteGeoJson(@NonNull @PathVariable Integer routeId,
                                                                       @RequestParam (defaultValue = "0.00001")Double tolerance,
                                                                          @AuthenticationPrincipal UserPrincipal user)
    {
        Instant startTime = Instant.now();
        GeoJsonFeatureCollection response = routeService.getRouteGeoJson(routeId,tolerance, user.getId());
        routeService.updateRecentlyViewedStatus(user.getId(),routeId);
        log.info("Fetched route geJson in {} ms", Duration.between(startTime,Instant.now()).toMillis());
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<RouteDetails>>> handleGetAllRoutes(@NonNull Integer page ,@NonNull Integer size)
    {
        ApiResponse<Page<RouteDetails>> response = routeService.getAllRoutes(page,size);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<RouteDetails>>> handleGetNearbyRoutes(@RequestParam Double longitude ,
                                                                                        @RequestParam Double latitude,
                                                                                        @RequestParam Double radiusMeters,
                                                                                        @RequestParam Integer limit)
    {
        NearbyRequest request = NearbyRequest.builder().
                longitude(longitude).latitude(latitude).radiusMeters(radiusMeters).limit(limit).
                build();
        ApiResponse<List<RouteDetails>> response = routeService.getNearbyRoutes(request);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @GetMapping("/recentlyViewed")
    public ResponseEntity<ApiResponse<Page<RouteDetails>>> handleGetRecentlyViewedRoutes(@AuthenticationPrincipal UserPrincipal user,
                                                                                   @RequestParam @NonNull Integer page,
                                                                                   @RequestParam @NonNull Integer size)
    {
        ApiResponse<Page<RouteDetails>> response = routeService.getRecentlyViewedRoutes(user.getId(),page,size);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);

    }
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<Page<RouteDetails>>> handleGetPopularRoutes(@RequestParam @NonNull Integer page,
                                                                                  @RequestParam @NonNull Integer size)
    {
        ApiResponse<Page<RouteDetails>> response = routeService.getPopularRoutes(page,size);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @GetMapping("/recentlyAdded")
    public ResponseEntity<ApiResponse<Page<RouteDetails>>> handleGetRecentlyAddedRoutes(@RequestParam @NonNull Integer page,
                                                                                        @RequestParam @NonNull Integer size)
    {
        ApiResponse<Page<RouteDetails>> response = routeService.getRecentlyAddedRoutes(page,size);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<RouteDetails>>> handleSearchRoutesByKeyword(@RequestParam @NonNull String keyword,
                                                                                      @RequestParam @NonNull Integer page,
                                                                                      @RequestParam @NonNull Integer size)
    {
        ApiResponse<Page<RouteDetails>> response = routeService.searchRoutesByKeyword(keyword,page,size);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }
    private HttpHeaders buildRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", UUID.randomUUID().toString());
        return headers;
    }
}
