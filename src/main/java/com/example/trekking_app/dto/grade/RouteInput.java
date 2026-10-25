package com.example.trekking_app.dto.grade;

import com.example.trekking_app.model.AlpineGrade;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.*;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RouteInput(
        String name,
        @Positive double distanceKm,
        @PositiveOrZero double elevationGainM,
        @NotNull @Positive Double maxAltitudeM,
        @PositiveOrZero Integer highPassM,
        boolean narrowOrExposed,
        boolean landslideOrRockfall,
        boolean scrambling,
        boolean glacierOrMoraine,
        boolean roughOrNoTrail,
        boolean campingRequired,
        boolean technicalGear,
        boolean peakClimb,
        AlpineGrade peakAlpineGrade) {}