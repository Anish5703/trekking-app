package com.example.trekking_app.controller;

import com.example.trekking_app.dto.destination.DestinationResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.service.DestinationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/destination")
public class DestinationController {

    private final DestinationService destinationService;

    public DestinationController(DestinationService destinationService)
    {
        this.destinationService = destinationService;
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<DestinationResponse>>> handleGetAllDestination()
    {
        ApiResponse<List<DestinationResponse>> response = destinationService.getAllDestination();
        return ResponseEntity.status(200).body(response);
    }
}
