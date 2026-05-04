package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.RouteRequest;
import com.example.trekking_app.dto.route.RouteResponse;
import com.example.trekking_app.model.UserPrincipal;
import com.example.trekking_app.service.RouteService;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/v1/admin/route")
public class AdminRouteController {

    private final RouteService routeService;

    public AdminRouteController(RouteService routeService )
    {
        this.routeService = routeService;

    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<RouteResponse>> handleCreateRoute(@Valid @RequestBody RouteRequest routeRequest,
                                                                        @AuthenticationPrincipal UserPrincipal user)
    {
        ApiResponse<RouteResponse> response = routeService.createRoute(routeRequest, user.getId());
        return ResponseEntity.status(201).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<RouteResponse>> handleGetRoute(@NonNull @RequestParam  Integer routeId)
    {
        ApiResponse<RouteResponse> response = routeService.getRoute(routeId);
        return  ResponseEntity.status(200).body(response);
    }


}
