package com.example.trekking_app.controller;

import com.example.trekking_app.dto.geojson.GeoJsonFeature;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.RouteDetails;
import com.example.trekking_app.dto.route.RouteResponse;
import com.example.trekking_app.service.RouteService;
import lombok.NonNull;
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

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<RouteDetails>>> handleGetAllRoute(@NonNull @RequestParam Integer destinationId)
    {
        ApiResponse<List<RouteDetails>> response = routeService.getAllRoute(destinationId);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<ApiResponse<RouteResponse>> handleGetRoute(@NonNull @PathVariable Integer routeId)
    {
        ApiResponse<RouteResponse> response = routeService.getRoute(routeId);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/{routeId}/path")
    public ResponseEntity<ApiResponse<GeoJsonFeature>> handleGetRoutePath(@NonNull @PathVariable Integer routeId)
    {
        ApiResponse<GeoJsonFeature> response = routeService.getRoutePath(routeId);
        return ResponseEntity.status(200).body(response);
    }


}
