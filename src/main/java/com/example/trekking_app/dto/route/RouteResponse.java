package com.example.trekking_app.dto.route;

import com.example.trekking_app.model.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private int  userId;
    private Integer estimatedDays;
    private LocalDateTime timeStamp;
}
