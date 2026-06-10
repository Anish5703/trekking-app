package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.route.*;
import com.example.trekking_app.entity.Destination;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.User;
import lombok.NonNull;
import org.locationtech.jts.geom.Point;

import java.util.List;

public class RouteMapper {

    public Route toEntity(@NonNull RouteRequest routeRequest, User user , Destination destination)
    {
        return   Route.builder()
                .name(routeRequest.getName())
                .destination(destination)
                .description(routeRequest.getDescription())
                .difficultyLevel(routeRequest.getDifficultyLevel())
                .user(user)
                .estimatedDays(routeRequest.getEstimatedDays())
                .build();

    }

    public RouteResponse toRouteResponse(@NonNull Route route, Point startCoords, Point endCoords, List<String> imageUrls)
    {
        double startLong = 0.0;
        double startLat = 0.0;
        double endLong = 0.0;
        double endLat = 0.0;
        if(startCoords!=null ) {
            startLong = startCoords.getX();
            startLat = startCoords.getY();
        }
        if(endCoords!=null) {
            endLong = endCoords.getX();
            endLat = endCoords.getY();
        }



         return RouteResponse.builder()
                 .id(route.getId())
                 .name(route.getName())
                 .destinationId(route.getDestination().getId())
                 .description(route.getDescription())
                 .difficultyLevel(route.getDifficultyLevel())
                 .estimatedDays(route.getEstimatedDays())
                 .maxElevation(route.getMaxElevation())
                 .minElevation(route.getMinElevation())
                 .startLongitude(startLong)
                 .startLatitude(startLat)
                 .endLongitude(endLong)
                 .endLatitude(endLat)
                 .totalDistanceInKm(route.getDistanceInKm())
                 .imageUrls(imageUrls)
                 .timeStamp(route.getTimeStamp())
                 .build();

    }

    public RouteDetails toRouteDetails(@NonNull Route route,Point endCoords,List<String> imageUrls)
    {
        double endLong = 0.0;
        double endLat = 0.0;
        if(endCoords!=null) {
            endLong = endCoords.getX();
            endLat = endCoords.getY();
        }

        return RouteDetails.builder()
                 .id(route.getId())
                 .name(route.getName())
                 .destinationId(route.getDestination().getId())
                 .estimatedDays(route.getEstimatedDays())
                 .difficultyLevel(route.getDifficultyLevel())
                 .maxElevation(route.getMaxElevation())
                 .totalDistanceInKm(route.getDistanceInKm())
                 .endLongitude(endLong)
                 .endLatitude(endLat)
                 .imageUrls(imageUrls)
                 .build();


    }
    public RouteDetails toNearbyRouteResponse(@NonNull NearbyRouteProjection projections)
    {
        double endLong = 0.0;
        double endLat = 0.0;
        if(endCoords!=null) {
            endLong = endCoords.getX();
            endLat = endCoords.getY();
        }

        return RouteDetails.builder()
                .id(route.getId())
                .name(route.getName())
                .destinationId(route.getDestination().getId())
                .estimatedDays(route.getEstimatedDays())
                .difficultyLevel(route.getDifficultyLevel())
                .maxElevation(route.getMaxElevation())
                .totalDistanceInKm(route.getDistanceInKm())
                .endLongitude(endLong)
                .endLatitude(endLat)
                .imageUrls(imageUrls)
                .build();
    }
}
