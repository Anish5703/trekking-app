package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.poi.XlsxImportResponse;
import com.example.trekking_app.service.xlsx.XlsxIngestionService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/route/{routeId}/xlsx")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminXlsxController {

    private final XlsxIngestionService xlsxIngestionService;

    @CacheEvict(value = "route-geoJson" ,key="#routeId" , allEntries = true)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<XlsxImportResponse>> handleUploadXlsx(@PathVariable Integer routeId,
                                                           @NonNull MultipartFile file)
    {
        ApiResponse<XlsxImportResponse> response = xlsxIngestionService.uploadXlsx(routeId,file);
        return ResponseEntity.status(201).body(response);
    }

    @CacheEvict(value = "route-geoJson" ,key="#routeId" , allEntries = true)
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> handleDeleteXlsxImports(@PathVariable Integer routeId)
    {
        ApiResponse<Void> response = xlsxIngestionService.deleteXlsxImports(routeId);
        return ResponseEntity.status(200).body(response);
    }

}
