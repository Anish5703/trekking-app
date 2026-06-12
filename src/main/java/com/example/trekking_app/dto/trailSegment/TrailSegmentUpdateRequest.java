package com.example.trekking_app.dto.trailSegment;

import com.example.trekking_app.model.TrailType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TrailSegmentUpdateRequest
{
    @NonNull
    private String name;
    @NonNull
    private TrailType type;
    private Integer estimatedTimeMinutes;
    private Double distanceInMeter;
}
