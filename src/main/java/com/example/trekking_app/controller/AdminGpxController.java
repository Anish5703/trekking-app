package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.gpx.GpxImportResponse;
import com.example.trekking_app.dto.gpx.GpxSegmentOrderRequest;
import com.example.trekking_app.dto.gpx.GpxSegmentResponse;
import com.example.trekking_app.model.GpxSegmentStatus;
import com.example.trekking_app.service.gpx.GpxIngestionService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/route/{routeId}/gpx")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminGpxController {

    private final GpxIngestionService orchestrator;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GpxSegmentResponse>>> handleGetAllGpxSegments(@PathVariable Integer routeId,
                                                                                        @NonNull @RequestParam GpxSegmentStatus segmentStatus)
    {
        ApiResponse<List<GpxSegmentResponse>> response = orchestrator.getAllGpxSegment(routeId,segmentStatus);
        return  ResponseEntity.status(200).body(response);
    }

    @GetMapping("/{gpxSegmentId}")
    public ResponseEntity<ApiResponse<GpxSegmentResponse>> handleGetGpxSegment(@PathVariable Integer routeId ,
                                                                                @PathVariable Integer gpxSegmentId)
    {
        ApiResponse<GpxSegmentResponse> response = orchestrator.getGpxSegment(routeId,gpxSegmentId);
        return ResponseEntity.status(200).body(response);
    }

    @PostMapping(value = "/upload" , consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<GpxImportResponse>>> handleUploadGpx(@PathVariable Integer routeId ,
                                                                         @RequestPart("files")List<MultipartFile> files,
                                                                                @NonNull @RequestParam GpxSegmentStatus segmentStatus) throws IOException
    {
        ApiResponse<List<GpxImportResponse>> response = orchestrator.uploadGpxFiles(routeId, files, segmentStatus);
        return ResponseEntity.status(201).body(response);

    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> handleDeleteGpxSegment(@NonNull @RequestParam Integer gpxSegmentId ,
                                                                    @PathVariable Integer routeId,
                                                                    @NonNull @RequestParam GpxSegmentStatus segmentStatus)
    {
        ApiResponse<Void> response = orchestrator.deleteGpxSegment(gpxSegmentId,routeId,segmentStatus);
        return ResponseEntity.status(200).body(response);
    }

    @PutMapping("/reorder")
    public ResponseEntity<ApiResponse<Void>> handleReorderGpxSegment(@RequestBody GpxSegmentOrderRequest orderRequest,
                                                                      @PathVariable Integer routeId,
                                                                     @NonNull @RequestParam GpxSegmentStatus segmentStatus)
    {
        ApiResponse<Void> response = orchestrator.reorderGpxSegment(orderRequest,routeId,segmentStatus);
        return ResponseEntity.status(200).body(response);
    }

    @PutMapping("/remerge")
    public ResponseEntity<ApiResponse<Void>> handleRemergeGpxSegment(@PathVariable Integer routeId,
                                                                     @NonNull @RequestParam GpxSegmentStatus segmentStatus)
    {
        ApiResponse<Void> response = orchestrator.remergeGpxSegment(routeId,segmentStatus);
        return ResponseEntity.status(200).body(response);
    }



}
