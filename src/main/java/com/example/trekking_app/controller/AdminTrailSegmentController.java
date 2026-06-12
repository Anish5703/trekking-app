package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.trailSegment.TrailSegmentResponse;
import com.example.trekking_app.dto.trailSegment.TrailSegmentUpdateRequest;
import com.example.trekking_app.service.trailSegment.TrailSegmentService;
import jakarta.validation.Valid;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/route/{routeId}/trailSegment")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTrailSegmentController
{

    private final TrailSegmentService trailSegmentService;


   @GetMapping
    public ResponseEntity<ApiResponse<Page<TrailSegmentResponse>>> handleGetAllTrailSegments(@PathVariable Integer routeId,
                                                                          @RequestParam(defaultValue = "0") Integer page,
                                                                          @RequestParam(defaultValue = "20") Integer size)
    {
        ApiResponse<Page<TrailSegmentResponse>> response = trailSegmentService.getAllTrailSegments(routeId,page,size);
        return ResponseEntity.status(200).body(response);
    }
    @GetMapping("/{trailSegmentId}")
    public ResponseEntity<ApiResponse<TrailSegmentResponse>> handleGetTrailSegment(@PathVariable Integer routeId,
                                                                                   @PathVariable Integer trailSegmentId)
    {
        ApiResponse<TrailSegmentResponse> response = trailSegmentService.getTrailSegment(routeId,trailSegmentId);
        return ResponseEntity.status(200).body(response);
    }
    @PutMapping("/{trailSegmentId}")
    public ResponseEntity<ApiResponse<TrailSegmentResponse>> handleUpdateTrailSegment(@PathVariable Integer routeId, @PathVariable Integer trailSegmentId,
                                                                                      @Valid @RequestBody TrailSegmentUpdateRequest updateRequest)
    {
        ApiResponse<TrailSegmentResponse> response = trailSegmentService.updateTrailSegment(routeId,trailSegmentId,updateRequest);
        return ResponseEntity.status(200).body(response);
    }

}
