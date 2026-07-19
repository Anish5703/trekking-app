package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.geoJson.GeoJsonFeature;
import com.example.trekking_app.dto.geoJson.GeoJsonFeatureCollection;
import com.example.trekking_app.dto.geoJson.GeoJsonGap;
import com.example.trekking_app.dto.geoJson.GeoJsonGeometry;
import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrailSegment;
import jakarta.annotation.Nonnull;
import lombok.NonNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import java.util.*;

public class GeoJsonMapper {

    private final String ROUTE_ID_PREFIX = "route:";
    private final String POI_ID_PREFIX = "poi:";
    private final String TRAIL_SEGMENT_ID_PREFIX = "trail_segment:";
    private final String ACCOMMODATION_ID_PREFIX = "accommodation:";

    private static final double GAP_THRESHOLD_METERS = 200.0;
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private record Segment(Coordinate[] coords, Double gapBeforeMeters) {}

    public GeoJsonFeatureCollection toGeoJson(@NonNull Route route, @Nonnull Double tolerance) {
        List<GeoJsonFeature> features = new ArrayList<>();
        List<GeoJsonGap> gaps = new ArrayList<>();

        features.addAll(buildRouteFeatures(route, tolerance, gaps));

        if (route.getTrailSegments() != null) {
            route.getTrailSegments().forEach(segment -> {
                List<GeoJsonFeature> segFeatures = buildTrailSegmentFeatures(segment, tolerance, gaps);
                if (segFeatures != null) features.addAll(segFeatures);
            });
        }

        if (route.getPois() != null) {
            route.getPois().stream().map(this::buildPoiFeature).forEach(features::add);
        }

        if (route.getAccommodations() != null) {
            route.getAccommodations().stream().map(this::buildAccommodationFeature).forEach(features::add);
        }

        return GeoJsonFeatureCollection.builder()
                .type("FeatureCollection")
                .features(features)
                .gaps(gaps.isEmpty() ? null : gaps)
                .build();
    }

    public List<GeoJsonFeature> buildRouteFeatures(@NonNull Route route, @NonNull Double tolerance,
                                                   List<GeoJsonGap> gapsOut) {
        LineString geom = route.getPath();
        if (geom == null) return List.of();

        Geometry simplifiedGeom = TopologyPreservingSimplifier.simplify(geom, tolerance);
        List<Segment> segments = splitByGap(simplifiedGeom.getCoordinates(), GAP_THRESHOLD_METERS, gapsOut);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("type", "route");
        properties.put("name", route.getName());
        properties.put("distance", route.getDistanceInKm());
        properties.put("maxElevation", route.getMaxElevation());

        return buildFeaturesFromSegments(segments, ROUTE_ID_PREFIX + route.getId(), properties);
    }

    public List<GeoJsonFeature> buildTrailSegmentFeatures(@NonNull TrailSegment segment, @NonNull Double tolerance,
                                                          List<GeoJsonGap> gapsOut) {
        LineString geom = segment.getPath();
        if (geom == null) return List.of();

        Geometry simplifiedGeom = TopologyPreservingSimplifier.simplify(geom, tolerance);
        List<Segment> segments = splitByGap(simplifiedGeom.getCoordinates(), GAP_THRESHOLD_METERS, gapsOut);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("type", "trail_segment");
        props.put("name", segment.getName());
        props.put("trailSegmentType", segment.getType());
        props.put("distanceInMeter", segment.getDistanceInMeter());

        return buildFeaturesFromSegments(segments, TRAIL_SEGMENT_ID_PREFIX + segment.getId(), props);
    }

    private List<GeoJsonFeature> buildFeaturesFromSegments(List<Segment> segments, String baseId,
                                                           Map<String, Object> properties) {
        List<GeoJsonFeature> features = new ArrayList<>(segments.size());
        boolean multi = segments.size() > 1;

        for (int i = 0; i < segments.size(); i++) {
            Coordinate[] coords = segments.get(i).coords();
            if (coords.length == 0) continue;

            String id = multi ? baseId + "-" + (i + 1) : baseId;
            GeoJsonGeometry geometry;

            if (coords.length == 1) {
                geometry = new GeoJsonGeometry("Point", List.of(coords[0].x, coords[0].y));
            } else {
                List<List<Double>> coordList = Arrays.stream(coords)
                        .map(c -> List.of(c.x, c.y))
                        .toList();
                geometry = new GeoJsonGeometry("LineString", coordList);
            }

            features.add(GeoJsonFeature.builder()
                    .id(id)
                    .geometry(geometry)
                    .properties(properties)
                    .build());
        }
        return features;
    }

    /**
     * Splits coords wherever the gap exceeds thresholdMeters, and appends each
     * detected gap (raw from/to coordinate + distance) to gapsOut.
     */
    private List<Segment> splitByGap(Coordinate[] coords, double thresholdMeters, List<GeoJsonGap> gapsOut) {
        List<Segment> segments = new ArrayList<>();
        if (coords.length == 0) return segments;

        List<Coordinate> current = new ArrayList<>();
        current.add(coords[0]);
        Double gapBeforeCurrent = null;

        for (int i = 1; i < coords.length; i++) {
            double dist = haversineMeters(coords[i - 1], coords[i]);
            if (dist > thresholdMeters) {
                segments.add(new Segment(current.toArray(new Coordinate[0]), gapBeforeCurrent));

                gapsOut.add(GeoJsonGap.builder()
                        .from(List.of(coords[i - 1].x, coords[i - 1].y))
                        .to(List.of(coords[i].x, coords[i].y))
                        .distanceMeters(Math.round(dist * 100) / 100.0)
                        .build());

                current = new ArrayList<>();
                gapBeforeCurrent = dist;
            }
            current.add(coords[i]);
        }
        segments.add(new Segment(current.toArray(new Coordinate[0]), gapBeforeCurrent));
        return segments;
    }

    private double haversineMeters(Coordinate c1, Coordinate c2) {
        double lat1 = Math.toRadians(c1.y);
        double lat2 = Math.toRadians(c2.y);
        double dLat = Math.toRadians(c2.y - c1.y);
        double dLon = Math.toRadians(c2.x - c1.x);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private GeoJsonFeature buildPoiFeature(@NonNull POI poi) {
        List<Double> coords = List.of(poi.getLongitude(), poi.getLatitude());
        GeoJsonGeometry geometry = new GeoJsonGeometry("Point", coords);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("type", "poi");
        props.put("name", poi.getName());
        props.put("elevation", poi.getElevation());
        props.put("poiType", poi.getType());
        return GeoJsonFeature.builder().id(POI_ID_PREFIX + poi.getId()).geometry(geometry).properties(props).build();
    }

    private GeoJsonFeature buildAccommodationFeature(Accommodation accommodation) {
        List<Double> coords = List.of(accommodation.getLongitude(), accommodation.getLatitude());
        GeoJsonGeometry geometry = new GeoJsonGeometry("Point", coords);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("type", "accommodation");
        props.put("name", accommodation.getName());
        props.put("accommodationType", accommodation.getType());
        props.put("totalRooms", accommodation.getTotalRooms());
        props.put("roomPriceForNepali", accommodation.getPriceNepali());
        props.put("roomPriceForForeigner", accommodation.getPriceForeigner());
        return GeoJsonFeature.builder().id(ACCOMMODATION_ID_PREFIX + accommodation.getId())
                .geometry(geometry).properties(props).build();
    }
}