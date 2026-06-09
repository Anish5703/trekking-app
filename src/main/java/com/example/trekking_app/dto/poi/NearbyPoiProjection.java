package com.example.trekking_app.dto.poi;

import com.example.trekking_app.entity.Route;

public interface NearbyPoiProjection
{
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
