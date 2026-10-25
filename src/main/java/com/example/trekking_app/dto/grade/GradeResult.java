package com.example.trekking_app.dto.grade;

import com.example.trekking_app.model.*;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GradeResult(
        DifficultyLevel grade,
        int level,
        RouteCategory category,
        int score,
        Breakdown breakdown,
        DifficultyLevel pointsGrade,
        List<Floor> floors,
        List<String> reasons,
        List<String> warnings,
        AlpineGrade peakAlpineGrade,
        int rulesVersion) {

    public record Breakdown(int distance, int gain, int steepness, int altitude, int terrain) {}
    public record Floor(DifficultyLevel grade, String why) {}
}