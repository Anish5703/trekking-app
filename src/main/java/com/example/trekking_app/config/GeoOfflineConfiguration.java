package com.example.trekking_app.config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
@Configuration
@EnableConfigurationProperties(GeoOfflineProperties.class)
public class GeoOfflineConfiguration {}
