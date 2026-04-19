package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.RouteRequest;
import com.example.trekking_app.dto.route.RouteResponse;
import com.example.trekking_app.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/routes")
public class AdminRouteController {

    private RouteService routeService;

    public AdminRouteController(RouteService routeService)
    {
        this.routeService = routeService;
    }

    @PostMapping("/add")
    @PreAuthorize(("hasRole(ADMIN)"))
    public ResponseEntity<ApiResponse<RouteResponse>> handleCreateRoute(@Valid @RequestBody RouteRequest routeRequest,
                                                                        @AuthenticationPrincipal int userId
    ) {
        ApiResponse<RouteResponse> response = routeService.createRoute(routeRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
