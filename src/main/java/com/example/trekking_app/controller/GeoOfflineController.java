package com.example.trekking_app.controller;

import com.example.trekking_app.config.GeoOfflineProperties;
import com.example.trekking_app.dto.geoOffline.LocationContextResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.service.GeoOfflineService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/geo")
public class GeoOfflineController {
    private final GeoOfflineService service;
    private final GeoOfflineProperties properties;
    public GeoOfflineController(GeoOfflineService service, GeoOfflineProperties properties) {
        this.service = service; this.properties = properties;
    }

    @GetMapping("/locate")
    public ResponseEntity<ApiResponse<LocationContextResponse>> locate(@RequestParam double lat, @RequestParam double lon) {
        if (!properties.isEnabled()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate())
            .body(service.locate(lat, lon));
    }
}
