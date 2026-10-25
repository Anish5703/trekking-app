package com.example.trekking_app.service.route;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ElevationGainCalculator {

    private final double noiseM;

    public ElevationGainCalculator(@Value("${trekking.contour.noise-threshold-m:3.0}") double noiseM) {
        this.noiseM = noiseM;
    }

    /** Total climbing, ignoring GPX noise smaller than noiseM. */
    public double gain(List<Double> elevations) {
        Double anchor = null;
        double gain = 0;
        for (Double e : elevations) {
            if (e == null) continue;
            if (anchor == null) { anchor = e; continue; }
            double d = e - anchor;
            if (d >= noiseM)       { gain += d; anchor = e; }
            else if (d <= -noiseM) { anchor = e; }
        }
        return gain;
    }
}