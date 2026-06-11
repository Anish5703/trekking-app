package com.example.trekking_app.dto.accommodation;

public interface NearbyAccommodationProjection {
    Integer getId();
    String getName();
    String getDescription();
    String getType();
    Double getLatitude();
    Double getLongitude();
    Double getElevation();
    Integer getRouteId();
    Double getDistanceMeters();
}
