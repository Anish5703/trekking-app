package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.poi.NearbyPoiProjection;
import com.example.trekking_app.dto.poi.NearbyPoiResponse;
import com.example.trekking_app.dto.poi.PoiRequest;
import com.example.trekking_app.dto.poi.PoiResponse;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.model.POIType;

import java.util.List;

public class PoiMapper {

    public POI toEntity(PoiRequest request, Route route)
    {
        return POI.builder().
                route(route).
                wayPoint(null).
                name(request.getName()).
                latitude(request.getLatitude()).
                longitude(request.getLongitude()).
                elevation(request.getElevation()).
                type(request.getType()).
                description(request.getDescription())
                .build();
    }
    public PoiResponse toPoiResponse(POI poi, List<String> imageUrls)
    {
        return PoiResponse.builder().
                id(poi.getId()).
                routeId(poi.getRoute().getId()).
                name(poi.getName()).
                latitude(poi.getLatitude()).
                longitude(poi.getLongitude()).
                elevation(poi.getElevation()).
                type(poi.getType()).
                description(poi.getDescription()).
                imageUrls(imageUrls)
                .build();
    }

    public NearbyPoiResponse toNearbyPoiResponse(NearbyPoiProjection poi, List<String> imageUrls)
    {
        return NearbyPoiResponse.builder().
                id(poi.getId()).
                routeId(poi.getRouteId()).
                name(poi.getName()).
                latitude(poi.getLatitude()).
                longitude(poi.getLongitude()).
                elevation(poi.getElevation()).
                type(POIType.valueOf(poi.getType())).
                description(poi.getDescription()).
                distanceMetersFromCurrent(poi.getDistanceMeters()).
                imageUrls(imageUrls)
                .build();


    }
}
