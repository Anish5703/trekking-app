package com.example.trekking_app.dto.route;

import com.example.trekking_app.model.DifficultyLevel;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RouteRequest {

    @NotBlank(message = "route title cannot be null")
    @Size(min = 5 , message = "route title is too short")
    @Size(max = 100 , message = "route title is too long")
    private String name;

    @NotNull(message = "route destination is required")
    private Integer destinationId;

    @NotBlank(message = "route description cannot be null")
    private String description;

    @NotNull(message = "route difficulty level required")
    private DifficultyLevel difficultyLevel;

//    @NotNull(message = "route estimated days required")
//    private Integer estimatedDays;
    private List<Integer> approxDays;
    private List<Integer> approxHours;
    private List<String> importantKeywords;
    private List<String> includedRegions;

    private LocalDateTime recordedAt;


}
