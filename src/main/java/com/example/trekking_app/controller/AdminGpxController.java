package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.GpxImportResponse;
import com.example.trekking_app.service.GpxParserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/gpx")
public class AdminGpxController {

    private final GpxParserService gpxParserService;

    public AdminGpxController(GpxParserService gpxParserService)
    {
        this.gpxParserService = gpxParserService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GpxImportResponse>> handleGpxImport(MultipartFile file,
                                                                          @RequestParam Integer routeId) {
        ApiResponse<GpxImportResponse> response = gpxParserService.importGpx(file,routeId);
        return ResponseEntity.status(201).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping( consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GpxImportResponse>> handleGpxUpdate(MultipartFile file,
                                                                          @RequestParam Integer routeId)
    {
        ApiResponse<GpxImportResponse> response = gpxParserService.importGpx(file,routeId);
        return ResponseEntity.status(200).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> handleGpxDelete(@RequestParam Integer routeId)
    {
        ApiResponse<Void> response = gpxParserService.deleteGpx(routeId);
        return ResponseEntity.status(200).body(response);
    }
}
