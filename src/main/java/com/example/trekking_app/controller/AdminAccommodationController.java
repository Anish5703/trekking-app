package com.example.trekking_app.controller;


import com.example.trekking_app.dto.accommodation.AccommodationRequest;
import com.example.trekking_app.dto.accommodation.AccommodationResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.service.accommodation.AccommodationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/route/{routeId}/accommodation")
@RequiredArgsConstructor
public class AdminAccommodationController {
    private final AccommodationService accommodationService;

@PostMapping()
    public ResponseEntity<ApiResponse<AccommodationResponse>> handleCreateAccommodation(@PathVariable Integer routeId,
                                                                                       @Valid @RequestBody AccommodationRequest accommodationRequest)
{
    ApiResponse<AccommodationResponse> response = accommodationService.createAccommodation(routeId,accommodationRequest);
    return ResponseEntity.status(201).headers(buildRequestHeaders()).body(response);

}

@PutMapping("/{accommodationId}")
    public ResponseEntity<ApiResponse<AccommodationResponse>> handleUpdateAccommodation(@PathVariable Integer routeId,
                                                                                        @PathVariable Integer accommodationId,
                                                                                        @Valid @RequestBody AccommodationRequest accommodationRequest)
{
    ApiResponse<AccommodationResponse> response = accommodationService.updateAccommodation(routeId,accommodationId,accommodationRequest);
    return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
}

@DeleteMapping("/{accommodationId}")
    public ResponseEntity<ApiResponse<Void>> handleDeleteAccommodation(@PathVariable Integer routeId,
                                                                       @PathVariable Integer accommodationId)
{
    ApiResponse<Void> response = accommodationService.deleteAccommodation(routeId,accommodationId);
    return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
}

    private HttpHeaders buildRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", UUID.randomUUID().toString());
        return headers;
    }

}
