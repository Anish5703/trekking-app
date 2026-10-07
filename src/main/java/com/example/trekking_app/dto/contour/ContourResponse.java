package com.example.trekking_app.dto.contour;

import lombok.*;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContourResponse implements Serializable {
    private Integer routeId;
    private Integer originalPointCount;   // trackpoints in the route
    private Integer pointCount;           // points returned after simplification
    private boolean elevationComplete;
    private Stats stats;                  // computed on ALL points, not the simplified list
    private List<Point> points;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Stats implements Serializable {
        private double elevationGain;
        private double elevationLoss;
        private Double minElevation;
        private Double maxElevation;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Point implements Serializable {
        private Double lat;
        private Double lon;
        private Double elevation;
    }
}