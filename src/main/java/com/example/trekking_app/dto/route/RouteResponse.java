package com.example.trekking_app.dto.route;

import com.example.trekking_app.model.DifficultyLevel;
import com.example.trekking_app.model.TrackPointStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.LineString;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteResponse {

    private Integer id;
    private String name;
    private int destinationId;
    private String description;
    private DifficultyLevel difficultyLevel;
    private Integer estimatedDays;
    private LocalDateTime timeStamp;
    private Double maxElevation;
    private Double minElevation;
    private Double totalDistanceInKm;

}
