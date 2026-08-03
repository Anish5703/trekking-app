package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.trackpoint.TrackPointRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointResponse;
import com.example.trekking_app.dto.trackpoint.TrackPointBatchRequest;
import com.example.trekking_app.service.trackpoints.TrackPointService;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
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

    @CacheEvict(value = "route-geoJson",key = "#routeId")
    @PutMapping("/{trackPointId}")
    public ResponseEntity<ApiResponse<TrackPointResponse>> updateTrackPoint(@PathVariable Integer routeId,
                                                                            @PathVariable Integer trackPointId,
                                                              @Valid @RequestBody TrackPointRequest trackPointRequest)
    {
        ApiResponse<TrackPointResponse> response = trackPointService.updateTrackPoint(routeId,trackPointId,trackPointRequest);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @CacheEvict(value = "route-geoJson",key = "#routeId")
    @DeleteMapping("/{trackPointId}")
    public ResponseEntity<ApiResponse<Void>> handleDeleteTrackPoint(@PathVariable Integer routeId ,
                                                                   @PathVariable Integer trackPointId)
    {
        ApiResponse<Void> response = trackPointService.deleteTrackPoint(routeId,trackPointId);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @CacheEvict(value = "route-geoJson",key = "#routeId")
    @DeleteMapping("/list")
    public ResponseEntity<ApiResponse<Void>> handleDeleteListOfTrackPoint(@PathVariable Integer routeId,
                                                                          @RequestBody TrackPointBatchRequest req)
    {
      ApiResponse<Void> response = trackPointService.deleteListOfTrackPoint(routeId,req);
      return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @CacheEvict(value = "route-geoJson",key = "#routeId")
    @PutMapping("/{trackPointId}/recover")
    public ResponseEntity<ApiResponse<Void>> handleUndoDeleteTrackPoint(@PathVariable Integer routeId,
                                                                        @PathVariable Integer trackPointId)
    {
        ApiResponse<Void> response = trackPointService.recoverTrackPoint(routeId,trackPointId);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @CacheEvict(value = "route-geoJson",key = "#routeId")
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
}
