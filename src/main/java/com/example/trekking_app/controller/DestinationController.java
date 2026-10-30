package com.example.trekking_app.controller;

import com.example.trekking_app.dto.destination.DestinationResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.RouteDetails;
import com.example.trekking_app.model.UserPrincipal;
import com.example.trekking_app.service.route.DestinationService;
import lombok.NonNull;
import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping("/{destinationId}")
    public ResponseEntity<ApiResponse<DestinationResponse>> handleGetDestination(@PathVariable Integer destinationId)
    {
        ApiResponse<DestinationResponse> response = destinationService.getDestination(destinationId);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<DestinationResponse>>> handleSearchDestinationByKeyword(@RequestParam @NonNull String keyword,
                                                                                                   @RequestParam @NonNull Integer page,
                                                                                                   @RequestParam @NonNull Integer size)
    {

        ApiResponse<Page<DestinationResponse>> response = destinationService.searchDestinationByKeyword(keyword,page,size);
        return ResponseEntity.status(200).body(response);
    }

}
