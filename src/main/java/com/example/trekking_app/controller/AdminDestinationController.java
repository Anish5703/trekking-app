package com.example.trekking_app.controller;

import com.example.trekking_app.dto.destination.DestinationRequest;
import com.example.trekking_app.dto.destination.DestinationResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.service.DestinationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(
            summary = "Create new destination",
            description = "Uses DestinationRequest dto to create new destination"
    )
    @ApiResponses(
            value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",description = "New destination Created"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Failed to create destination")
            })

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<DestinationResponse>> handleCreateDestination(@Valid @RequestBody DestinationRequest destinationRequest)
    {
        ApiResponse<DestinationResponse> response = destinationService.createDestination(destinationRequest);
        return ResponseEntity.status(201).body(response);
    }
    @Operation(
            summary = "Update existing destination",
            description = "Uses DestinationRequest dto and destinationId to update destination"
    )
    @ApiResponses(
            value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",description = "Destination updated"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Failed to update destination")
            })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<ApiResponse<DestinationResponse>> handleUpdateDestination(@RequestParam Integer destinationId,
                                                                                        @Valid @RequestBody DestinationRequest destinationRequest)
    {
        ApiResponse<DestinationResponse> response = destinationService.updateDestination(destinationRequest,destinationId);
        return ResponseEntity.status(200).body(response);
    }

    @Operation(
            summary = "Delete existing destination",
            description = "Uses destinationId  to delete destination"
    )
    @ApiResponses(
            value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",description = "Destination deleted"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Failed to delete destination")
            })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> handleDeleteDestination(@RequestParam int destinationId)
    {
        ApiResponse<Void> response = destinationService.deleteDestination(destinationId);
        return ResponseEntity.status(200).body(response);
    }
}
