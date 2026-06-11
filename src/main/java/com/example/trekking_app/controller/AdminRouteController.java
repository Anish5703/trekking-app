package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.RouteRequest;
import com.example.trekking_app.dto.route.RouteResponse;
import com.example.trekking_app.model.UserPrincipal;
import com.example.trekking_app.service.route.RouteService;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/admin/route")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRouteController {

    private final RouteService routeService;

    public AdminRouteController(RouteService routeService )
    {
        this.routeService = routeService;

    }


    @PostMapping
    public ResponseEntity<ApiResponse<RouteResponse>> handleCreateRoute(@Valid @RequestBody RouteRequest routeRequest,
                                                                        @AuthenticationPrincipal UserPrincipal user)
    {
        ApiResponse<RouteResponse> response = routeService.createRoute(routeRequest, user.getId());
        return ResponseEntity.status(201).headers(buildRequestHeaders()).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<RouteResponse>> handleGetRoute(@NonNull @RequestParam  Integer routeId)
    {
        ApiResponse<RouteResponse> response = routeService.getRoute(routeId);
        return  ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @PutMapping("/{routeId}")
    public ResponseEntity<ApiResponse<RouteResponse>> handleUpdateRoute(@NonNull @PathVariable Integer routeId ,
                                                                        @NonNull @RequestBody RouteRequest routeRequest,
                                                                        @AuthenticationPrincipal UserPrincipal user)
    {
        ApiResponse<RouteResponse> response = routeService.updateRoute(routeId,routeRequest,user.getId());
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @DeleteMapping("/{routeId}")
    public ResponseEntity<ApiResponse<Void>> handleDeleteRoute(@NonNull @PathVariable Integer routeId)
    {
        ApiResponse<Void> response = routeService.deleteRoute(routeId);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    private HttpHeaders buildRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", UUID.randomUUID().toString());
        return headers;
    }
}
