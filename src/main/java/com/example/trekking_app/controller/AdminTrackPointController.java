package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.trackpoint.TrackPointDetails;
import com.example.trekking_app.service.TrackPointService;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/trackpoint")
public class AdminTrackPointController {

    private final TrackPointService trackPointService;

    public AdminTrackPointController(TrackPointService trackPointService)
    {
        this.trackPointService = trackPointService;
    }

@GetMapping
public ResponseEntity<ApiResponse<List<TrackPointDetails>>> handleGetTrackPoints(@NonNull @RequestParam Integer routeId)
    {
       ApiResponse<List<TrackPointDetails>> response = trackPointService.getTrackPoints(routeId);
       return ResponseEntity.status(200).body(response);
    }

}
