package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.waypoint.WayPointResponse;
import com.example.trekking_app.service.waypoint.WayPointService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/route/{routeId}/waypoint")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminWayPointController {

    private final WayPointService wayPointService;

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Page<WayPointResponse>>> handleGetRouteAllWayPoints(@NonNull @PathVariable Integer routeId,
                                                                                         @RequestParam Integer page ,
                                                                                          @RequestParam Integer size)
    {
        ApiResponse<Page<WayPointResponse>> response = wayPointService.getAllWayPoints(routeId,page,size);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/active/list")
    public ResponseEntity<ApiResponse<Page<WayPointResponse>>> handleGetActiveTrackPoints(@PathVariable Integer routeId,
                                                                                            @NonNull  Integer page ,
                                                                                            @NonNull  Integer size)
    {
        ApiResponse<Page<WayPointResponse>> response = wayPointService.getActiveWayPoints(routeId,page,size);
        return ResponseEntity.status(200).body(response);
    }
    @GetMapping("/inactive/list")
    public ResponseEntity<ApiResponse<Page<WayPointResponse>>> handleGetInActiveTrackPoints(@PathVariable Integer routeId,
                                                                                              @NonNull Integer page,
                                                                                              @NonNull Integer size)
    {
        ApiResponse<Page<WayPointResponse>> response = wayPointService.getInactiveWayPoints(routeId,page,size);
        return ResponseEntity.status(200).body(response);
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> handleDeleteTrackPoint(@PathVariable Integer routeId ,
                                                                    @NonNull @RequestParam Integer trackPointId)
    {
        ApiResponse<Void> response = wayPointService.deleteWayPoint(routeId,trackPointId);
        return ResponseEntity.status(200).body(response);
    }

}
