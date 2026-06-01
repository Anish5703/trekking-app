package com.example.trekking_app.dto.route;
import com.example.trekking_app.model.DifficultyLevel;
import com.example.trekking_app.model.RouteStatus;

public interface NearbyRouteProjection
{
    Integer getId();
    String getName();
    String getDescription();
    DifficultyLevel getDifficultyLevel();
    Double getDistanceInKm();
    Integer getEstimatedDays();
    RouteStatus getRouteStatus();
    String getDestinationName();
    Double getDistanceMeters();
}
