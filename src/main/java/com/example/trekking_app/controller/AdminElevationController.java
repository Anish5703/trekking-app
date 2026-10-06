package com.example.trekking_app.controller;

import com.example.trekking_app.dto.contour.PopulateElevationResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.service.contour.ElevationPopulateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/route/{routeId}")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminElevationController {

    private final ElevationPopulateService populateService;

    @PostMapping("/elevation/populate")
    public ResponseEntity<ApiResponse<PopulateElevationResponse>> populate(@PathVariable int routeId) {
        ApiResponse<PopulateElevationResponse> response = populateService.populate(routeId);
        return  ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    private HttpHeaders buildRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", UUID.randomUUID().toString());
        return headers;
    }

}
