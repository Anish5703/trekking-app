package com.example.trekking_app.model;
/**
 * Status of offline map data generation for a route/region.
 * PENDING = admin requested offline bundle generation
 * PROCESSING = server is building tile pack + data bundle
 * READY = downloadable by mobile clients
 * FAILED = generation failed, admin must retry
 * EXPIRED = data is outdated, needs regeneration
 */
public enum OfflineRegionStatus {
    PENDING,
    PROCESSING,
    READY,
    FAILED,
    EXPIRED
}
