package com.example.trekking_app.dto.route;

import com.example.trekking_app.model.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RouteDetails {
    private Integer id;
    private String name;
    private int destinationId;
    private String description;
    private DifficultyLevel difficultyLevel;
    private Integer estimatedDays;
    private Double maxElevation;
    private Double minElevation;
    private Double totalDistanceInKm;

}
