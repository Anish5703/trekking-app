package com.example.trekking_app.controller;
import com.example.trekking_app.dto.geoJson.GeoJsonFeature;
import com.example.trekking_app.dto.geoJson.GeoJsonFeatureCollection;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.RouteDetails;
import com.example.trekking_app.dto.route.RouteRequest;
import com.example.trekking_app.dto.route.RouteResponse;
import com.example.trekking_app.service.route.RouteService;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/{routeId}/path")
    public ResponseEntity<ApiResponse<GeoJsonFeatureCollection>> handleGetRoutePath(@NonNull @PathVariable Integer routeId)
    {
        ApiResponse<GeoJsonFeatureCollection> response = routeService.getRoutePath(routeId);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<RouteDetails>>> handleGetAllRoutes(@NonNull Integer page ,@NonNull Integer size)
    {
        ApiResponse<Page<RouteDetails>> response = routeService.getAllRoutes(page,size);
        return ResponseEntity.status(200).body(response);
    }

}
