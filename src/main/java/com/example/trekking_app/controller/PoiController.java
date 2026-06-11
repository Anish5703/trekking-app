package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.poi.PoiResponse;
import com.example.trekking_app.dto.route.NearbyRequest;
import com.example.trekking_app.service.poi.PoiService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/route/{routeId}/poi")
@RequiredArgsConstructor
public class PoiController {
    private final PoiService poiService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PoiResponse>>> handleGetAllPoi(@PathVariable Integer routeId,
                                                                          @RequestParam Integer page ,
                                                                          @RequestParam Integer size)
    {
        ApiResponse<Page<PoiResponse>> response = poiService.getAllPoi(routeId,page,size);
        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/{poiId}")
    public ResponseEntity<ApiResponse<PoiResponse>> handleGetPoi(@PathVariable Integer routeId,
                                                                 @PathVariable Integer poiId)
    {
        ApiResponse<PoiResponse> response = poiService.getPoi(routeId, poiId);
        return ResponseEntity.status(200).body(response);
    }
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<PoiResponse>>> handleGetNearbyPoi(@RequestParam Double longitude ,
                                                                                   @RequestParam Double latitude,
                                                                                   @RequestParam Double radiusMeters,
                                                                                   @RequestParam Integer limit)
    {
        NearbyRequest request = NearbyRequest.builder().
                longitude(longitude).latitude(latitude).radiusMeters(radiusMeters).limit(limit).
                build();
        ApiResponse<List<PoiResponse>> response = poiService.getPoiNearby(request);
        return ResponseEntity.status(200).body(response);
    }

}
