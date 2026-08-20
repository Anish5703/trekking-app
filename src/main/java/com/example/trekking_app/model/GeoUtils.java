package com.example.trekking_app.model;

public final class GeoUtils {
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private GeoUtils() {}

    public static double haversineMeters(double lon1, double lat1, double lon2, double lat2) {
        double la1 = Math.toRadians(lat1), la2 = Math.toRadians(lat2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(la1) * Math.cos(la2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}