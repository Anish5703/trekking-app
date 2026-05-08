package com.example.trekking_app.mapper;


import com.example.trekking_app.dto.geojson.GeoJsonFeature;
import com.example.trekking_app.dto.geojson.GeoJsonGeometry;
import com.example.trekking_app.entity.Route;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GeoJsonMapper {
    public GeoJsonFeature toGeoJson(Route route) {

        // Step 1: LineString → coordinates
        List<List<Double>> coordinates = Arrays.stream(route.getPath().getCoordinates())
                .map(coord -> List.of(coord.x, coord.y))
                .toList();

        // Step 2: wrap in geometry
        GeoJsonGeometry geometry = new GeoJsonGeometry("LineString", coordinates);

        // Step 3: build properties
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("routeId", route.getId());
        properties.put("name", route.getName());

        // Step 4: return single feature
        return GeoJsonFeature.builder().geometry(geometry).properties(properties).build();
    }
}