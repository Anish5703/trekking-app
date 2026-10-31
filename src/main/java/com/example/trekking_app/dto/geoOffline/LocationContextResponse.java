package com.example.trekking_app.dto.geoOffline;

import java.util.List;

public record LocationContextResponse(double latitude, double longitude, Boundary municipality, Boundary district,
                                      Boundary province, List<ProtectedAreaSummary> protectedAreas) {
    public record Boundary(String sourceCode, String name) {}
    public record ProtectedAreaSummary(String sourceCode, String name, String type, Double areaSqKm) {}
}
