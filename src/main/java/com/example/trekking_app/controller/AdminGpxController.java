package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.gpx.GpxImportResponse;
import com.example.trekking_app.dto.gpx.GpxSegmentResponse;
import com.example.trekking_app.service.IngestionOrchestratorService;
import lombok.NonNull;
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
public class AdminGpxController {

    private final IngestionOrchestratorService orchestrator;

    public AdminGpxController(IngestionOrchestratorService orchestrator)
    {
        this.orchestrator = orchestrator;
    }

    @PostMapping(value = "/upload" , consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<GpxImportResponse>>> handleUploadGpx(@PathVariable Integer routeId ,
                                                                         @RequestPart("files")List<MultipartFile> files) throws IOException
    {
        ApiResponse<List<GpxImportResponse>> response = orchestrator.uploadGpxFiles(routeId, files);
        return ResponseEntity.status(201).body(response);

    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> handleDeleteGpxSegment(@NonNull @RequestParam Integer gpxSegmentId , @PathVariable Integer routeId)
    {
        ApiResponse<Void> response = orchestrator.deleteGpxSegment(gpxSegmentId,routeId);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GpxSegmentResponse>>> handleGetAllGpxSegments(@PathVariable Integer routeId)
    {
        ApiResponse<List<GpxSegmentResponse>> response = orchestrator.getAllGpxSegment(routeId);
        return  ResponseEntity.status(200).body(response);
    }



}
