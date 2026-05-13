package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.gpx.GpxSegmentOrderRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointResponse;
import com.example.trekking_app.service.GpxParserService;
import com.example.trekking_app.service.IngestionOrchestratorService;
import com.example.trekking_app.service.TrackPointService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/route/{routeId}/trackpoint")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTrackPointController {

    private final TrackPointService trackPointService;


@GetMapping("/all")
public ResponseEntity<ApiResponse<Page<TrackPointResponse>>> handleGetAlTrackPoints(@PathVariable Integer routeId,
                                                                                    Integer page ,
                                                                                    Integer size)
    {
       ApiResponse<Page<TrackPointResponse>> response = trackPointService.getAllTrackPoints(routeId,page,size);
       return ResponseEntity.status(200).body(response);
    }
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Page<TrackPointResponse>>> handleGetActiveTrackPoints(@PathVariable Integer routeId,
                                                                                          @NonNull  Integer page ,
                                                                                          @NonNull  Integer size)
    {
        ApiResponse<Page<TrackPointResponse>> response = trackPointService.getActiveTrackPoints(routeId,page,size);
        return ResponseEntity.status(200).body(response);
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateTrackPoint(@PathVariable Integer routeId,
                                                              @NonNull @RequestParam Integer trackPointId,
                                                              @NonNull @RequestBody TrackPointRequest trackPointRequest)
    {
        ApiResponse<Void> response = trackPointService.updateTrackPoint(routeId,trackPointId,trackPointRequest);
        return ResponseEntity.status(200).body(response);
    }
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> handleDeleteTrackPoint(@PathVariable Integer routeId ,
                                                                   @NonNull @RequestParam Integer trackPointId)
    {
        ApiResponse<Void> response = trackPointService.deleteTrackPoint(routeId,trackPointId);
        return ResponseEntity.status(200).body(response);
    }
}
