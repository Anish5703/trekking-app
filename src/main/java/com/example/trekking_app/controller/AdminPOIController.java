package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.poi.POIUploadResponse;
import com.example.trekking_app.service.xlsx.XlsxIngestionService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/v1/admin/route/{routeId}")
@RequiredArgsConstructor
public class AdminPOIController {

private final XlsxIngestionService ingestionService;

    @PostMapping(value = "/gpx/{gpxSegmentId}/poi/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<POIUploadResponse>> handleUploadXlsxFile(@NonNull @PathVariable Integer routeId ,
                                                                              @NonNull @PathVariable Integer gpxSegmentId,
                                                                              MultipartFile file)
    {
        ApiResponse<POIUploadResponse> response = ingestionService.uploadXlsxFile(routeId, gpxSegmentId, file);
        return ResponseEntity.status(201).body(response);

    }


}
