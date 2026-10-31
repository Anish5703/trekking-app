package com.example.trekking_app.dto.geoOffline;
public record OfflinePackageResponse(byte[] body, String etag, long contentLength, boolean notModified) {}
