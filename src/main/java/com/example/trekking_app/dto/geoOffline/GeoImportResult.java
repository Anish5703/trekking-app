package com.example.trekking_app.dto.geoOffline;
import java.time.Instant;
public record GeoImportResult(String status, int provinces, int districts, int municipalities, int protectedAreas,
                              String adminSha256, String protectedAreaSha256, Instant importedAt) {}
