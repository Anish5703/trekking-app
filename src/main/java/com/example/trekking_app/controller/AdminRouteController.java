package com.example.trekking_app.controller;

import com.cloudinary.Api;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.RouteDetails;
import com.example.trekking_app.dto.route.RouteRequest;
import com.example.trekking_app.dto.route.RouteResponse;
import com.example.trekking_app.dto.route.RouteVerificationRequest;
import com.example.trekking_app.model.UserPrincipal;
import com.example.trekking_app.model.VerificationStatus;
import com.example.trekking_app.service.route.RouteService;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


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
        return ResponseEntity.status(201).headers(buildRequestHeaders()).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<RouteResponse>> handleGetRoute(@NonNull @RequestParam  Integer routeId,
                                                                     @AuthenticationPrincipal UserPrincipal user)
    {
        ApiResponse<RouteResponse> response = routeService.getRoute(routeId, user.getId());
        return  ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{routeId}")
    public ResponseEntity<ApiResponse<RouteResponse>> handleUpdateRoute(@NonNull @PathVariable Integer routeId ,
                                                                        @NonNull @RequestBody RouteRequest routeRequest,
                                                                        @AuthenticationPrincipal UserPrincipal user)
    {
        ApiResponse<RouteResponse> response = routeService.updateRoute(routeId,routeRequest,user.getId());
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{routeId}")
    public ResponseEntity<ApiResponse<Void>> handleDeleteRoute(@NonNull @PathVariable Integer routeId)
    {
        ApiResponse<Void> response = routeService.deleteRoute(routeId);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{routeId}/publish")
    public ResponseEntity<ApiResponse<Void>> handleUpdateRoutePublishStatus(@PathVariable Integer routeId,@RequestParam(defaultValue = "true") Boolean publish)
    {
        ApiResponse<Void>  response = routeService.updateRoutePublishStatus(routeId,publish);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @PutMapping("/{routeId}/lock")
    public ResponseEntity<ApiResponse<Void>> handleUpdateRouteLockStatus(@PathVariable Integer routeId,@RequestParam(defaultValue = "true") Boolean lock)
    {
        ApiResponse<Void>  response = routeService.updateRouteLockStatus(routeId,lock);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{routeId}/verfication")
    public ResponseEntity<ApiResponse<Void>> handleUpdateRouteVerificationStatus(@PathVariable Integer routeId
                                                                                 ,@RequestBody RouteVerificationRequest req,
                                                                                 @AuthenticationPrincipal UserPrincipal user)
    {
        ApiResponse<Void> response = routeService.updateRouteVerificationStatus(routeId,req,user.getId());
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);

    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<RouteDetails>>> handleGetAdminCreatedRoutes(@PathVariable @NonNull Integer userId ,
                                                                                       @RequestParam @NonNull Integer page ,
                                                                                       @RequestParam @NonNull Integer size)
    {
        ApiResponse<Page<RouteDetails>> response = routeService.getAdminCreatedRoutes(userId,page,size);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }



    private HttpHeaders buildRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", UUID.randomUUID().toString());
        return headers;
    }
}
