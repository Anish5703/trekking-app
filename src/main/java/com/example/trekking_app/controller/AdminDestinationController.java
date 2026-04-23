package com.example.trekking_app.controller;

import com.example.trekking_app.dto.destination.DestinationRequest;
import com.example.trekking_app.dto.destination.DestinationResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.service.DestinationService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/destination")
public class AdminDestinationController {

    private final DestinationService destinationService;

    public AdminDestinationController(DestinationService destinationService)
    {
        this.destinationService = destinationService;
    }


    @GetMapping
    public ResponseEntity<ApiResponse<List<DestinationResponse>>> handleGetAllDestination()
    {
        ApiResponse<List<DestinationResponse>> response = destinationService.getAllDestination();
        return ResponseEntity.status(200).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<DestinationResponse>> handleCreateDestination(@Valid @RequestBody DestinationRequest destinationRequest)
    {
        ApiResponse<DestinationResponse> response = destinationService.createDestination(destinationRequest);
        return ResponseEntity.status(201).body(response);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<ApiResponse<DestinationResponse>> handleUpdateDestination(@RequestParam int destinationId,
                                                                                        @Valid @RequestBody DestinationRequest destinationRequest)
    {
        ApiResponse<DestinationResponse> response = destinationService.updateDestination(destinationRequest,destinationId);
        return ResponseEntity.status(200).body(response);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Integer>> handleDeleteDestination(@RequestParam int destinationId)
    {
        ApiResponse<Integer> response = destinationService.deleteDestination(destinationId);
        return ResponseEntity.status(200).body(response);
    }
}
