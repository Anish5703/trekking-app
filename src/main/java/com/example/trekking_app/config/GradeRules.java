package com.example.trekking_app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "trek.grade")
public record GradeRules(int rulesVersion, Scale distance, Scale gain, Scale steepness,
                         Scale altitude, Terrain terrain, GradeCuts gradeCuts, Category category) {

    public record Scale(List<Double> cuts, List<Integer> points) {
        public int band(double v) {
            for (int i = 0; i < cuts.size(); i++) if (v < cuts.get(i)) return points.get(i);
            return points.get(points.size() - 1);
        }
    }
    public record Terrain(int highPass, int narrowOrExposed, int landslideOrRockfall, int scrambling,
                          int glacierOrMoraine, int roughOrNoTrail, int camping, int cap) {}
    public record GradeCuts(int moderate, int hard, int veryHard, int expert) {}
    public record Category(double hikeMaxDistanceKm, double hikeMaxGainM) {}
}