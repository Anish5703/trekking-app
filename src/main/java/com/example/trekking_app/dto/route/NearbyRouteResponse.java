package com.example.trekking_app.dto.route;

import com.example.trekking_app.model.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class NearbyRouteResponse {

    private Integer routeId;
    private String routeName;
    private String description;
    private DifficultyLevel difficultyLevel;
    private String destinationName;
    private Integer estimatedDays;
    private Double routeDistanceInKm;
    private Double distanceToStartPoint;
}
