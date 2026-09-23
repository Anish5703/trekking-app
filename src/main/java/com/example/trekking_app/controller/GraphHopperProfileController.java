package com.example.trekking_app.controller;

import com.example.trekking_app.service.graphHopper.GraphHopperProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/graphhopper")
@RequiredArgsConstructor
public class GraphHopperProfileController {

    private final GraphHopperProfileService graphHopperProfileService;

    @GetMapping("/profiles")
    public ResponseEntity<List<String>> getProfiles() {
        return ResponseEntity.ok(graphHopperProfileService.getAvailableProfiles());
    }
}