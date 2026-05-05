package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.gpx.GpxSegmentOrderRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointResponse;
import com.example.trekking_app.service.GpxParserService;
import com.example.trekking_app.service.IngestionOrchestratorService;
import com.example.trekking_app.service.TrackPointService;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/route/{routeId}/trackpoint")
public class AdminTrackPointController {

    private final TrackPointService trackPointService;
    private final IngestionOrchestratorService orchestrator;
    private final GpxParserService gpxParserService;

    public AdminTrackPointController(TrackPointService trackPointService , IngestionOrchestratorService orchestrator,
                                     GpxParserService gpxParserService)
    {
        this.trackPointService = trackPointService;
        this.orchestrator = orchestrator;
        this.gpxParserService = gpxParserService;
    }

@GetMapping("/all")
public ResponseEntity<ApiResponse<Page<TrackPointResponse>>> handleGetAlTrackPoints(@PathVariable Integer routeId,int page , int size)
    {
       ApiResponse<Page<TrackPointResponse>> response = trackPointService.getAllTrackPoints(routeId,page,size);
       return ResponseEntity.status(200).body(response);
    }
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Page<TrackPointResponse>>> handleGetActiveTrackPoints(@PathVariable Integer routeId,int page , int size)
    {
        ApiResponse<Page<TrackPointResponse>> response = trackPointService.getActiveTrackPoints(routeId,page,size);
        return ResponseEntity.status(200).body(response);
    }

    @PutMapping("/reorder")
    public ResponseEntity<ApiResponse<Void>> handleReorderTrackPoints(@RequestBody GpxSegmentOrderRequest orderRequest,
                                                                    @PathVariable Integer routeId)
    {
        ApiResponse<Void> response = orchestrator.reorderGpxSegment(orderRequest,routeId);
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
