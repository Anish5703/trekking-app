package com.example.trekking_app.dto.geoOffline;
public record OfflinePackageManifest(long routeId, String etag, long contentLength, double corridorMeters,
                                     double simplifyMeters, String schemaVersion) {}
