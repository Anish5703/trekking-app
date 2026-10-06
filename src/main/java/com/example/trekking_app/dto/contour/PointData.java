package com.example.trekking_app.dto.contour;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Lightweight trackpoint snapshot used internally (not exposed by the API). */
@Getter
@Builder
@AllArgsConstructor
public class PointData {
    private final Integer id;
    private final Double lat;
    private final Double lon;
    private final Double elevation;

    public boolean hasValidCoordinates() {
        return lat != null && lon != null
                && Double.isFinite(lat) && Double.isFinite(lon)
                && Math.abs(lat) <= 90 && Math.abs(lon) <= 180;
    }

    /** Present only if non-null and non-zero (GPS exports often use 0 for "unknown"). */
    public boolean hasElevation() {
        return elevation != null && elevation != 0.0;
    }

    public boolean needsElevation() {
        return !hasElevation() && hasValidCoordinates();
    }
}