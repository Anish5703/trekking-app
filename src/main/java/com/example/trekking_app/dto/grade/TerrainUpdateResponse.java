package com.example.trekking_app.dto.grade;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TerrainUpdateResponse(TerrainProfile terrain, GradeResult grade, List<String> missingInputs) {}