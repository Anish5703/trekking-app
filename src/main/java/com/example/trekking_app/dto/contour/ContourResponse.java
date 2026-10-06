package com.example.trekking_app.dto.contour;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContourResponse implements Serializable {
    private Integer routeId;
    private Integer pointCount;
    private boolean elevationComplete;   // false if some points still have no elevation
    private Stats stats;
    private List<Point> points;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats implements Serializable {
        private double elevationGain;
        private double elevationLoss;
        private Double minElevation;
        private Double maxElevation;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Point implements Serializable {
        private Double lat;
        private Double lon;
        private Double elevation;
    }
}