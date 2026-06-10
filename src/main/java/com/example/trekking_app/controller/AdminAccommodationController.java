package com.example.trekking_app.controller;


import com.example.trekking_app.dto.accommodation.AccommodationRequest;
import com.example.trekking_app.dto.accommodation.AccommodationResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.service.accommodation.AccommodationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/route/{routeId}/accommodation")
@RequiredArgsConstructor
public class AdminAccommodationController {
    private final AccommodationService accommodationService;

@PostMapping()
    public ResponseEntity<ApiResponse<AccommodationResponse>> handleCreateAccommodation(@PathVariable Integer routeId,
                                                                                       @Valid @RequestBody AccommodationRequest accommodationRequest)
{
    ApiResponse<AccommodationResponse> response = accommodationService.createAccommodation(routeId,accommodationRequest);
    return ResponseEntity.status(201).body(response);

}

}
