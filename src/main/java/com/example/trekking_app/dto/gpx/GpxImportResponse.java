package com.example.trekking_app.dto.gpx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class GpxImportResponse {

    private Integer routeId;
    private Integer segmentId;
    private String  segmentName;
    private Integer totalTrackPoints;
    private Integer totalWayPoints;
    private Double totalDistanceInKm;
}
