package com.example.trekking_app.dto.route;

import com.example.trekking_app.model.DifficultyLevel;
import com.example.trekking_app.model.RouteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RouteDetails {
    private Integer id;
    private String name;
    private int destinationId;
    private DifficultyLevel difficultyLevel;
    private Integer estimatedDays;
    private Double maxElevation;
    private Double endLongitude;
    private Double endLatitude;
    private Double totalDistanceInKm;
    private List<String> imageUrls;
    private Boolean isPublished;

}
