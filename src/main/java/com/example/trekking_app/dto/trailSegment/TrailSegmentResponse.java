package com.example.trekking_app.dto.trailSegment;

import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.WayPoint;
import com.example.trekking_app.model.TrailType;
import jakarta.persistence.*;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TrailSegmentResponse {

    private Integer id;
    private String name;
    private Integer routeId;
    private TrailType type;
    private Integer startWaypointId;
    private Integer endWaypointId;
    private String color;
    private Integer gpxSegmentId;
    private Integer estimatedTimeMinutes;
    private Double distanceInMeter;
}
