package com.example.trekking_app.controller;
import com.example.trekking_app.dto.contour.ContourResponse;
import com.example.trekking_app.dto.geoJson.GeoJsonFeatureCollection;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.grade.GradeResult;
import com.example.trekking_app.dto.polyline.PolylineFeatureCollectionResponse;
import com.example.trekking_app.dto.route.*;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.exception.auth.UserNotFoundException;
import com.example.trekking_app.exception.resource.NoResourceFoundException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.model.GraphHopperProfile;
import com.example.trekking_app.model.Role;
import com.example.trekking_app.model.UserPrincipal;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.UserRepository;
import com.example.trekking_app.service.contour.ElevationProfileService;
import com.example.trekking_app.service.route.PolylineEncodingService;
import com.example.trekking_app.service.route.RouteGradeUpdater;
import com.example.trekking_app.service.route.RouteService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;
    private final ElevationProfileService profileService;
    private final PolylineEncodingService polylineEncodingService;
    private final RouteGradeUpdater gradeUpdater;
    private final RouteRepository routeRepo;
    private final UserRepository userRepo;



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
                                                                          @RequestParam(defaultValue = "200.0")Double thresholdMeters,
                                                                          @AuthenticationPrincipal UserPrincipal user,
                                                                          @RequestParam (defaultValue = "foot" ) GraphHopperProfile graphHopperProfile)
    {
        Instant startTime = Instant.now();
        int userId = user.getId();
        filterUnpublishedRoute(userId,routeId);
        GeoJsonFeatureCollection response = routeService.getRouteGeoJson(routeId,tolerance,thresholdMeters, user.getId(),graphHopperProfile);
        routeService.updateRecentlyViewedStatus(user.getId(),routeId);
        log.info("Fetched route geJson in {} ms", Duration.between(startTime,Instant.now()).toMillis());
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }
    @GetMapping("/{routeId}/contour")
    public ResponseEntity<ApiResponse<ContourResponse>> handleGetRouteContour(@NonNull @PathVariable Integer routeId,
                                                                              @RequestParam (defaultValue = "0.5")Double tolerance,@AuthenticationPrincipal UserPrincipal user)
    {
        Instant startTime = Instant.now();
        int userId = user.getId();
        filterUnpublishedRoute(userId,routeId);
        // clamp to 0..1000 m and snap to 0.5 m steps: bounds the number of cache keys per route
        double tol = Math.round(Math.min(Math.max(tolerance, 0), 1000) * 2) / 2.0;
        ApiResponse<ContourResponse> response = profileService.getContour(routeId,tol);
        log.info("Fetched route contour in {} ms", Duration.between(startTime,Instant.now()).toMillis());
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
                                                                                        @RequestParam Integer limit,
                                                                                 @AuthenticationPrincipal UserPrincipal user)
    {
        NearbyRequest request = NearbyRequest.builder().
                longitude(longitude).latitude(latitude).radiusMeters(radiusMeters).limit(limit).
                build();
        ApiResponse<List<RouteDetails>> response = routeService.getNearbyRoutes(request,user.getId());
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
                                                                                  @RequestParam @NonNull Integer size,
                                                                                  @AuthenticationPrincipal UserPrincipal user)
    {
        ApiResponse<Page<RouteDetails>> response = routeService.getPopularRoutes(page,size,user.getId());

        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @GetMapping("/recentlyAdded")
    public ResponseEntity<ApiResponse<Page<RouteDetails>>> handleGetRecentlyAddedRoutes(@RequestParam @NonNull Integer page,
                                                                                        @RequestParam @NonNull Integer size,
                                                                                        @AuthenticationPrincipal UserPrincipal user)
    {
        ApiResponse<Page<RouteDetails>> response = routeService.getRecentlyAddedRoutes(page,size,user.getId());
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

    @GetMapping("{routeId}/polyline")
    public ResponseEntity<PolylineFeatureCollectionResponse> handleGetRoutePolyline(@NonNull @PathVariable Integer routeId,
                                                                                    @RequestParam (defaultValue = "0.00001")Double tolerance,
                                                                                    @RequestParam(defaultValue = "200.0")Double thresholdMeters,
                                                                                    @AuthenticationPrincipal UserPrincipal user,
                                                                                    @RequestParam (defaultValue = "foot" ) GraphHopperProfile graphHopperProfile)
    {
        Instant startTime = Instant.now();
        int userId = user.getId();
        filterUnpublishedRoute(userId,routeId);
        GeoJsonFeatureCollection geoJson = routeService.getRouteGeoJson(routeId,tolerance,thresholdMeters, user.getId(),graphHopperProfile);
        PolylineFeatureCollectionResponse response = polylineEncodingService.encodeFeatureCollection(geoJson);

        routeService.updateRecentlyViewedStatus(user.getId(), routeId);

        log.info(
                "Fetched encoded route polyline for route {} in {} ms",
                routeId,
                Duration.between(startTime, Instant.now()).toMillis()
        );

        return ResponseEntity.status(200)
                .headers(buildRequestHeaders())
                .body(response);
    }

    @GetMapping("/{routeId}/grade")
    @Operation(summary = "Get the stored grade of a route. 404 if the route is missing or not graded yet.")
    public ResponseEntity<ApiResponse<GradeResult>> handleGetRouteGrade(@PathVariable Integer routeId) {
        ApiResponse<GradeResult> response = gradeUpdater.getGrade(routeId);
        return ResponseEntity.status(response.getStatus()).headers(buildRequestHeaders()).body(response);
    }
    private HttpHeaders buildRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", UUID.randomUUID().toString());
        return headers;
    }

    //helper method to filter unpublishedRoute
    private void filterUnpublishedRoute(Integer userId , Integer routeId)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        User user = userRepo.findById(userId).orElseThrow(() -> new UserNotFoundException("user not found with id "+userId));
        if(!route.getIsPublished())
            if(user.getRole().equals(Role.CUSTOMER)) throw new NoResourceFoundException("route is not published yet");
    }
}
