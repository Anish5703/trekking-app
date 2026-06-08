package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.poi.PoiRequest;
import com.example.trekking_app.dto.poi.PoiResponse;
import com.example.trekking_app.service.poi.PoiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/admin/route/{routeId}/poi")
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPoiController {

    private final PoiService poiService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PoiResponse>>> handleGetAllPoi(@PathVariable Integer routeId,
                                                                          @RequestParam Integer page ,
                                                                          @RequestParam Integer size)
    {
        ApiResponse<Page<PoiResponse>> response = poiService.getAllPoi(routeId,page,size);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/{poiId}")
    public ResponseEntity<ApiResponse<PoiResponse>> handleGetPoi(@PathVariable Integer routeId,
                                                                 @PathVariable Integer poiId)
    {
        ApiResponse<PoiResponse> response = poiService.getPoi(routeId, poiId);
        return ResponseEntity.status(200).body(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PoiResponse>> handleCreatePoi(@PathVariable Integer routeId,
                                                    @Valid @RequestBody PoiRequest poiRequest)
    {
        ApiResponse<PoiResponse> response = poiService.createPoi(routeId,poiRequest);
        return ResponseEntity.status(201).body(response);
    }

    @DeleteMapping("/{poiId}")
    public ResponseEntity<ApiResponse<Void>> handleDeletePoi(@PathVariable Integer routeId,
                                                             @PathVariable Integer poiId)
    {
        ApiResponse<Void> response = poiService.deletePoi(routeId,poiId);
        return ResponseEntity.status(200).body(response);

    }
    @PutMapping("/{poiId}")
    public ResponseEntity<ApiResponse<PoiResponse>> handleUpdatePoi(@PathVariable Integer routeId,
                                                             @PathVariable Integer poiId,
                                                                    @Valid @RequestBody PoiRequest poiRequest)
    {
        ApiResponse<PoiResponse> response = poiService.updatePoi(routeId,poiId,poiRequest);
        return ResponseEntity.status(200).body(response);

    }

}
