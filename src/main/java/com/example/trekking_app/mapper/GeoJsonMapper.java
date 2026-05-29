package com.example.trekking_app.mapper;


import com.example.trekking_app.dto.geoJson.GeoJsonFeature;
import com.example.trekking_app.dto.geoJson.GeoJsonFeatureCollection;
import com.example.trekking_app.dto.geoJson.GeoJsonGeometry;
import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrailSegment;

import java.util.*;

public class GeoJsonMapper {

    private final String ROUTE_ID_PREFIX = "route:";
    private final String POI_ID_PREFIX = "poi:";
    private final String TRAIL_SEGMENT_ID_PREFIX = "trail_segment:";
    private final String ACCOMMODATION_ID_PREFIX = "accommodation:";

    public GeoJsonFeatureCollection toGeoJson(Route route) {
        List<GeoJsonFeature> features = new ArrayList<>();

        features.add(buildRouteFeature(route));

        if (route.getTrailSegments() != null) {
            route.getTrailSegments().stream()
                    .map(this::buildTrailSegmentFeature)
                    .forEach(features::add);
        }

        if (route.getPois() != null) {
            route.getPois().stream()
                    .map(this::buildPoiFeature)
                    .forEach(features::add);
        }

        if (route.getAccommodations() != null) {
            route.getAccommodations().stream()
                    .map(this::buildAccommodationFeature)
                    .forEach(features::add);
        }

        return new GeoJsonFeatureCollection("FeatureCollection", features);
    }

    public GeoJsonFeature buildRouteFeature(Route route) {

        List<List<Double>> coords = Arrays.stream(route.getPath().getCoordinates())
                .map(coord -> List.of(coord.x, coord.y))
                .toList();

        GeoJsonGeometry geometry = new GeoJsonGeometry("LineString", coords);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("type","route");
        properties.put("name", route.getName());
        properties.put("distance", route.getDistanceInKm());
        properties.put("maxElevation",route.getMaxElevation());


        return GeoJsonFeature.builder().
                id(ROUTE_ID_PREFIX+route.getId()).
                geometry(geometry).
                properties(properties).build();
    }

    public GeoJsonFeature buildTrailSegmentFeature(TrailSegment segment)
    {
        if(segment.getPath()==null) return null;
        List<List<Double>> coords = Arrays.stream(segment.getPath().getCoordinates())
                         .map(c -> List.of(c.x,c.y))
                        .toList();

        GeoJsonGeometry geometry = new GeoJsonGeometry("LineString",coords);
        Map<String,Object> props = new LinkedHashMap<>();
        props.put("type", "trail_segment");
        props.put("name", segment.getName());
       props.put("trailSegmentType",segment.getType());
        props.put("distanceInMeter",segment.getDistanceInMeter());
        return GeoJsonFeature.builder().
                id(TRAIL_SEGMENT_ID_PREFIX+segment.getId()).
                geometry(geometry).properties(props).build();

    }

    private GeoJsonFeature buildPoiFeature(POI poi) {
        List<Double> coords = List.of(
                poi.getLongitude(),
                poi.getLatitude()
        );

        GeoJsonGeometry geometry = new GeoJsonGeometry("Point", coords);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("type", "poi");
        props.put("name", poi.getName());
        props.put("elevation", poi.getElevation());
        props.put("poiType", poi.getType());
        return GeoJsonFeature.builder().
                id(POI_ID_PREFIX+poi.getId()).
                geometry(geometry).
                properties(props).build();
    }
    private GeoJsonFeature buildAccommodationFeature(Accommodation accommodation) {
        List<Double> coords = List.of(
                accommodation.getLongitude(),
                accommodation.getLatitude()
        );

        GeoJsonGeometry geometry = new GeoJsonGeometry("Point", coords);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("type", "accommodation");
        props.put("name", accommodation.getName());
        props.put("accommodationType", accommodation.getType());
        props.put("totalRooms", accommodation.getTotalRooms());
        props.put("roomPriceForNepali",accommodation.getPriceNepali());
        props.put("roomPriceForForeigner",accommodation.getPriceForeigner());
        return GeoJsonFeature.builder().
                id(ACCOMMODATION_ID_PREFIX+accommodation.getId()).
                geometry(geometry).properties(props).build();
    }


}