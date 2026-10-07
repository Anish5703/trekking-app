package com.example.trekking_app.service.contour;

import com.example.trekking_app.dto.contour.ContourResponse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** 3D Douglas-Peucker (east, north, elevation in metres). Keeps summits/valleys; always keeps first and last point. */
public final class ContourSimplifier {
    private static final double METERS_PER_DEGREE = 111_320.0;

    private ContourSimplifier() {}

    public static List<ContourResponse.Point> simplify(List<ContourResponse.Point> points, double toleranceMeters) {
        int n = points.size();
        if (n <= 2 || toleranceMeters <= 0) return points;

        // project to local metres
        double meanLat = 0;
        for (var p : points) meanLat += p.getLat();
        meanLat /= n;
        double kx = METERS_PER_DEGREE * Math.cos(Math.toRadians(meanLat));
        double lon0 = points.get(0).getLon();
        double lat0 = points.get(0).getLat();

        double[] x = new double[n], y = new double[n], z = fillElevations(points);
        for (int i = 0; i < n; i++) {
            x[i] = (points.get(i).getLon() - lon0) * kx;
            y[i] = (points.get(i).getLat() - lat0) * METERS_PER_DEGREE;
        }

        // iterative Douglas-Peucker (no recursion, safe for very long routes)
        double tol2 = toleranceMeters * toleranceMeters;
        boolean[] keep = new boolean[n];
        keep[0] = keep[n - 1] = true;
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{0, n - 1});

        while (!stack.isEmpty()) {
            int[] s = stack.pop();
            int a = s[0], b = s[1];
            if (b <= a + 1) continue;

            double maxD2 = -1;
            int idx = -1;
            for (int i = a + 1; i < b; i++) {
                double d2 = distSq(x[i], y[i], z[i], x[a], y[a], z[a], x[b], y[b], z[b]);
                if (d2 > maxD2) { maxD2 = d2; idx = i; }
            }
            if (maxD2 > tol2) {
                keep[idx] = true;
                stack.push(new int[]{a, idx});
                stack.push(new int[]{idx, b});
            }
        }

        List<ContourResponse.Point> out = new ArrayList<>();
        for (int i = 0; i < n; i++) if (keep[i]) out.add(points.get(i));
        return out;
    }

    /** Squared distance from P to segment AB in 3D. */
    private static double distSq(double px, double py, double pz,
                                 double ax, double ay, double az,
                                 double bx, double by, double bz) {
        double vx = bx - ax, vy = by - ay, vz = bz - az;
        double wx = px - ax, wy = py - ay, wz = pz - az;
        double vv = vx * vx + vy * vy + vz * vz;
        double t = vv == 0 ? 0 : Math.max(0, Math.min(1, (wx * vx + wy * vy + wz * vz) / vv));
        double dx = wx - t * vx, dy = wy - t * vy, dz = wz - t * vz;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Null elevations carried forward (only for the distance maths; output keeps the original values). */
    private static double[] fillElevations(List<ContourResponse.Point> pts) {
        int n = pts.size();
        double[] z = new double[n];
        Double last = null;
        for (int i = 0; i < n; i++) {
            Double e = pts.get(i).getElevation();
            if (e != null) last = e;
            z[i] = last == null ? Double.NaN : last;
        }
        int first = 0;
        while (first < n && Double.isNaN(z[first])) first++;
        double lead = first < n ? z[first] : 0.0;
        for (int i = 0; i < first; i++) z[i] = lead;
        return z;
    }
}