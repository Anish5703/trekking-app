package com.example.trekking_app.dto.grade;

import com.example.trekking_app.model.AlpineGrade;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TerrainProfile {
    @Min(0) @Max(9000)
    private Integer highPassM;
    private boolean narrowOrExposed;
    private boolean landslideOrRockfall;
    private boolean scrambling;
    private boolean glacierOrMoraine;
    private boolean roughOrNoTrail;
    private boolean campingRequired;
    private boolean technicalGear;
    private boolean peakClimb;
    private AlpineGrade peakAlpineGrade;

    @JsonIgnore   // keep out of the JSONB column and API payloads
    @AssertTrue(message = "peak_alpine_grade requires peak_climb = true")
    public boolean isPeakGradeConsistent() {
        return peakAlpineGrade == null || peakClimb;
    }
}