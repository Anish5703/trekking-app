package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.RouteResponse;
import com.example.trekking_app.service.RouteService;
import lombok.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<ApiResponse<List<RouteResponse>>> handleGetAllRoute(@NonNull @RequestParam Integer destinationId)
    {
        ApiResponse<List<RouteResponse>> response = routeService.getAllRoute(destinationId);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<RouteResponse>> handleGetRoute(@NonNull @RequestParam Integer routeId)
    {
        ApiResponse<RouteResponse> response = routeService.getRoute(routeId);
        return ResponseEntity.status(200).body(response);
    }
}
