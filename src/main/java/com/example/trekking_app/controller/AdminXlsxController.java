package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.poi.XlsxImportResponse;
import com.example.trekking_app.service.xlsx.XlsxIngestionService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/route/{routeId}/xlsx")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminXlsxController {

    private final XlsxIngestionService xlsxIngestionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<XlsxImportResponse>> handleUploadXlsx(@PathVariable Integer routeId,
                                                           @NonNull MultipartFile file)
    {
        ApiResponse<XlsxImportResponse> response = xlsxIngestionService.uploadXlsx(routeId,file);
        return ResponseEntity.status(201).body(response);
    }

}
