package com.example.trekking_app.service.contour;
import com.example.trekking_app.dto.contour.ContourResponse;

import java.util.List;

final class ElevationStatsCalculator {
    private ElevationStatsCalculator() {}

    /**
     * Gain/loss with hysteresis: a rise or fall counts only once it exceeds {@code threshold} metres
     * from the last anchor. Filters DEM/GPS noise. Null elevations are skipped. Min/max use raw values.
     */
    static ContourResponse.Stats compute(List<Double> elevations, double threshold) {
        double gain = 0, loss = 0;
        Double min = null, max = null, anchor = null;

        for (Double e : elevations) {
            if (e == null) continue;
            min = (min == null) ? e : Math.min(min, e);
            max = (max == null) ? e : Math.max(max, e);
            if (anchor == null) { anchor = e; continue; }

            double diff = e - anchor;
            if (diff >= threshold)       { gain += diff;  anchor = e; }
            else if (diff <= -threshold) { loss += -diff; anchor = e; }
        }
        return ContourResponse.Stats.builder()
                .elevationGain(round1(gain))
                .elevationLoss(round1(loss))
                .minElevation(round1(min))
                .maxElevation(round1(max))
                .build();
    }

    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private static Double round1(Double v) { return v == null ? null : round1(v.doubleValue()); }
}
