package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.gpx.GapPreviewDto;
import com.example.trekking_app.dto.route.FillGapsRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointCreateRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointResponse;
import com.example.trekking_app.dto.trackpoint.TrackPointBatchRequest;
import com.example.trekking_app.model.GraphHopperProfile;
import com.example.trekking_app.service.trackpoints.TrackPointService;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/route/{routeId}/trackpoint")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTrackPointController {

    private final TrackPointService trackPointService;


@GetMapping("/list")
public ResponseEntity<ApiResponse<Page<TrackPointResponse>>> handleGetAlTrackPoints(@PathVariable Integer routeId,
                                                                                   @NonNull Integer page ,
                                                                                   @NonNull Integer size)
    {
       ApiResponse<Page<TrackPointResponse>> response = trackPointService.getAllTrackPoints(routeId,page,size);
       return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/{trackpointId}")
    public ResponseEntity<ApiResponse<TrackPointResponse>> handleGetTrackPoint(@PathVariable Integer routeId,
                                                                               @PathVariable Integer trackpointId)
    {
        ApiResponse<TrackPointResponse> response = trackPointService.getTrackpoint(routeId,trackpointId);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TrackPointResponse>>> handleGetTrackPointByLatAndLon(@PathVariable Integer routeId,
                                                                                                @NonNull Double latitude,
                                                                                                @NonNull Double longitude)
    {
        ApiResponse<List<TrackPointResponse>> response = trackPointService.getTrackPointByLatAndLong(routeId,latitude,longitude);
        return ResponseEntity.status(200).body(response);
    }
    @GetMapping("/active/list")
    public ResponseEntity<ApiResponse<Page<TrackPointResponse>>> handleGetActiveTrackPoints(@PathVariable Integer routeId,
                                                                                          @NonNull  Integer page ,
                                                                                          @NonNull  Integer size)
    {
        ApiResponse<Page<TrackPointResponse>> response = trackPointService.getActiveTrackPoints(routeId,page,size);
        return ResponseEntity.status(200).body(response);
    }
    @GetMapping("/inactive/list")
    public ResponseEntity<ApiResponse<Page<TrackPointResponse>>> handleGetInActiveTrackPoints(@PathVariable Integer routeId,
                                                                                            @NonNull Integer page,
                                                                                              @NonNull Integer size)
    {
        ApiResponse<Page<TrackPointResponse>> response = trackPointService.getInactiveTrackPoints(routeId,page,size);
        return ResponseEntity.status(200).body(response);
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<Void>> handleCreateTrackPoint(@PathVariable Integer routeId ,
                                                                    @RequestBody TrackPointCreateRequest tpCreateReq)
    {
        ApiResponse<Void> response = trackPointService.createTrackpoint(routeId,tpCreateReq);
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{trackPointId}")
    public ResponseEntity<ApiResponse<TrackPointResponse>> updateTrackPoint(@PathVariable Integer routeId,
                                                                            @PathVariable Integer trackPointId,
                                                              @Valid @RequestBody TrackPointRequest trackPointRequest)
    {
        ApiResponse<TrackPointResponse> response = trackPointService.updateTrackPoint(routeId,trackPointId,trackPointRequest);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @DeleteMapping("/{trackPointId}")
    public ResponseEntity<ApiResponse<Void>> handleDeleteTrackPoint(@PathVariable Integer routeId ,
                                                                   @PathVariable Integer trackPointId)
    {
        ApiResponse<Void> response = trackPointService.deleteTrackPoint(routeId,trackPointId);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @DeleteMapping("/list")
    public ResponseEntity<ApiResponse<Void>> handleDeleteListOfTrackPoint(@PathVariable Integer routeId,
                                                                          @RequestBody TrackPointBatchRequest req)
    {
      ApiResponse<Void> response = trackPointService.deleteListOfTrackPoint(routeId,req);
      return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @PutMapping("/{trackPointId}/recover")
    public ResponseEntity<ApiResponse<Void>> handleUndoDeleteTrackPoint(@PathVariable Integer routeId,
                                                                        @PathVariable Integer trackPointId)
    {
        ApiResponse<Void> response = trackPointService.recoverTrackPoint(routeId,trackPointId);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @PutMapping("/list/recover")
    public ResponseEntity<ApiResponse<Void>> handleUndoDeleteListOfTrackPoint(@PathVariable Integer routeId,
                                                                              @RequestBody TrackPointBatchRequest req)
    {
        ApiResponse<Void> response = trackPointService.recoverTrackPoints(routeId,req);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    private HttpHeaders buildRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", UUID.randomUUID().toString());
        return headers;
    }

    @GetMapping("/gaps")
    public ResponseEntity<ApiResponse<List<GapPreviewDto>>> handlePreviewGaps(
            @PathVariable Integer routeId,
            @RequestParam(defaultValue = "200") double thresholdMeters) {
        List<GapPreviewDto> response = trackPointService.detectGaps(routeId, thresholdMeters);
        return ResponseEntity.status(200).body(new ApiResponse<>(response,"previewing gaps",200));
    }

    @PostMapping("/gaps/fill")
    public ResponseEntity<ApiResponse<Void>> handleFillGaps(
            @PathVariable Integer routeId,
            @RequestParam(defaultValue = "foot") GraphHopperProfile profile,
            @Valid @RequestBody FillGapsRequest request) {
        ApiResponse<Void> response = trackPointService.fillGaps(routeId, request , profile);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @GetMapping("/lastPrecedingId")
    public ResponseEntity<ApiResponse<Integer>> handleGetLastTrackpointId(@PathVariable Integer routeId)
    {
        ApiResponse<Integer> response = trackPointService.getLastPrecedingTrackpointId(routeId);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }
}
