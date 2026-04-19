package com.example.trekking_app.dto.route;

import com.example.trekking_app.model.DifficultyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RouteRequest {

    @NotBlank(message = "route title cannot be null")
    @Size(min = 5 , message = "route title is too short")
    @Size(max = 100 , message = "route title is too long")
    private String title;

    @NotBlank(message = "route description cannot be null")
    private String description;

    @NotNull(message = "route difficulty level required")
    private DifficultyLevel difficultyLevel;

    @NotNull(message = "route estimated days required")
    private Integer estimatedDays;

    @NotBlank(message = "route region required")
    private String region;

    @NotBlank(message = "route district required")
    private String district;

}
