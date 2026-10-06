package com.example.trekking_app.dto.contour;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "elevation")
public record ElevationProperties(
        String baseUrl,
        @DefaultValue("") String apiKey,
        @DefaultValue("srtm30m") String dataset,
        @DefaultValue("500") int batchSize,
        @DefaultValue("5s") Duration timeout) {}
