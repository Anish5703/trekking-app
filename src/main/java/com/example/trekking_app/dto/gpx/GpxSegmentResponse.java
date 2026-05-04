package com.example.trekking_app.dto.gpx;

import com.example.trekking_app.model.GpxSegmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpxSegmentResponse {
    private Integer id;
    private Integer routeId;
    private Integer orderIndex;
    private String  sourceFilename;
    private GpxSegmentStatus status;
    private Integer trackPointCount;
    private Integer wayPointCount;
    private Double distanceInKm;
    private LocalDateTime minTime;
    private LocalDateTime maxTime;
}
