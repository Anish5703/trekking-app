package com.example.trekking_app.service.route;

import com.example.trekking_app.dto.geoJson.GeoJsonFeature;
import com.example.trekking_app.dto.geoJson.GeoJsonFeatureCollection;
import com.example.trekking_app.dto.geoJson.GeoJsonGeometry;
import com.example.trekking_app.dto.polyline.PolylineFeatureCollectionResponse;
import com.example.trekking_app.dto.polyline.PolylineFeatureResponse;
import com.example.trekking_app.dto.polyline.PolylineGeometryResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PolylineEncodingService {

    private static final int PRECISION = 6;
    private static final long FACTOR = 1_000_000L;

    public PolylineFeatureCollectionResponse encodeFeatureCollection(
            GeoJsonFeatureCollection geoJson) {

        if (geoJson == null) {
            throw new IllegalArgumentException(
                    "GeoJSON feature collection cannot be null");
        }

        List<PolylineFeatureResponse> encodedFeatures = new ArrayList<>();

        if (geoJson.getFeatures() != null) {
            for (GeoJsonFeature feature : geoJson.getFeatures()) {

                if (feature == null) {
                    continue;
                }

                PolylineGeometryResponse geometry =
                        encodeGeometry(feature.getGeometry());

                encodedFeatures.add(
                        PolylineFeatureResponse.builder()
                                .id(feature.getId())
                                .type(feature.getType())
                                .properties(feature.getProperties())
                                .geometry(geometry)
                                .build()
                );
            }
        }

        return PolylineFeatureCollectionResponse.builder()
                .type("FeatureCollection")
                .encoding("google-polyline")
                .precision(PRECISION)
                .features(encodedFeatures)
                .gaps(geoJson.getGaps())
                .build();
    }

    private PolylineGeometryResponse encodeGeometry(
            GeoJsonGeometry geometry) {

        if (geometry == null) {
            return null;
        }

        String geometryType = geometry.getType();
        Object coordinates = geometry.getCoordinates();

        if ("LineString".equals(geometryType)) {

            List<double[]> points = extractPoints(coordinates);

            return PolylineGeometryResponse.builder()
                    .type("EncodedPolyline")
                    .coordinates(encodePolyline(points))
                    .precision(PRECISION)
                    .build();
        }

        // Point and other geometry types remain unchanged.
        return PolylineGeometryResponse.builder()
                .type(geometryType)
                .coordinates(coordinates)
                .build();
    }

    private List<double[]> extractPoints(Object coordinates) {

        if (!(coordinates instanceof List<?> coordinateList)) {
            throw new IllegalArgumentException(
                    "LineString coordinates must be a list");
        }

        List<double[]> points = new ArrayList<>(coordinateList.size());

        for (Object coordinate : coordinateList) {

            if (!(coordinate instanceof List<?> values)
                    || values.size() < 2
                    || !(values.get(0) instanceof Number longitude)
                    || !(values.get(1) instanceof Number latitude)) {

                throw new IllegalArgumentException(
                        "Invalid LineString coordinate");
            }

            // GeoJSON uses [longitude, latitude].
            // Polyline encoding requires [latitude, longitude].
            points.add(new double[]{
                    latitude.doubleValue(),
                    longitude.doubleValue()
            });
        }

        return points;
    }

    public String encodePolyline(List<double[]> points) {

        StringBuilder result = new StringBuilder();

        long previousLatitude = 0;
        long previousLongitude = 0;

        for (double[] point : points) {

            if (point == null || point.length < 2) {
                throw new IllegalArgumentException(
                        "Each point must contain latitude and longitude");
            }

            double latitude = point[0];
            double longitude = point[1];

            validateCoordinates(latitude, longitude);

            long currentLatitude = Math.round(latitude * FACTOR);
            long currentLongitude = Math.round(longitude * FACTOR);

            long latitudeDelta = currentLatitude - previousLatitude;
            long longitudeDelta = currentLongitude - previousLongitude;

            encodeValue(latitudeDelta, result);
            encodeValue(longitudeDelta, result);

            previousLatitude = currentLatitude;
            previousLongitude = currentLongitude;
        }

        return result.toString();
    }

    private void validateCoordinates(
            double latitude,
            double longitude) {

        if (!Double.isFinite(latitude)
                || !Double.isFinite(longitude)
                || latitude < -90.0
                || latitude > 90.0
                || longitude < -180.0
                || longitude > 180.0) {

            throw new IllegalArgumentException(
                    "Invalid latitude or longitude");
        }
    }

    private void encodeValue(long delta, StringBuilder result) {

        long value = delta < 0
                ? ~(delta << 1)
                : (delta << 1);

        while (value >= 0x20) {

            int chunk = (int) ((value & 0x1F) | 0x20);

            result.append((char) (chunk + 63));

            value >>>= 5;
        }

        result.append((char) (value + 63));
    }
}