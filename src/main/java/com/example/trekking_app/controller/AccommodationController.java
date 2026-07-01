package com.example.trekking_app.controller;

import com.example.trekking_app.dto.accommodation.AccommodationResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.NearbyRequest;
import com.example.trekking_app.service.accommodation.AccommodationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/route/{routeId}/accommodation")
@RequiredArgsConstructor
public class AccommodationController
{
    private final AccommodationService accommodationService;

    @GetMapping("/{accommodationId}")
    public ResponseEntity<ApiResponse<AccommodationResponse>> handleGetAccommodation(@PathVariable Integer routeId,
                                                                                     @PathVariable Integer accommodationId) {
        ApiResponse<AccommodationResponse> response = accommodationService.getAccommodation(routeId, accommodationId);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AccommodationResponse>>> handleGetAllAccommodation(@PathVariable Integer routeId,
                                                                                              @RequestParam Integer page,
                                                                                              @RequestParam Integer size)
    {
        ApiResponse<Page<AccommodationResponse>> response = accommodationService.getAllAccommodation(routeId,page,size);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<AccommodationResponse>>> handleGetListOfAccommodation(@PathVariable Integer routeId)
    {
        ApiResponse<List<AccommodationResponse>> response = accommodationService.getListOfAccommodation(routeId);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<AccommodationResponse>>> handleGetNearbyAccommodation(@RequestParam Double longitude ,
                                                                                                       @RequestParam Double latitude,
                                                                                                       @RequestParam Double radiusMeters,
                                                                                                       @RequestParam Integer limit)
    {
        NearbyRequest request = NearbyRequest.builder().
                longitude(longitude).latitude(latitude).radiusMeters(radiusMeters).limit(limit).
                build();
        ApiResponse<List<AccommodationResponse>> response = accommodationService.getAccommodationNearby(request);
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    private HttpHeaders buildRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", UUID.randomUUID().toString());
        return headers;
    }
}
