package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.route.*;
import com.example.trekking_app.entity.Destination;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.User;
import lombok.NonNull;

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

    public RouteResponse toRouteResponse(@NonNull Route route)
    {
         return RouteResponse.builder()
                 .id(route.getId())
                 .name(route.getName())
                 .destinationId(route.getDestination().getId())
                 .description(route.getDescription())
                 .difficultyLevel(route.getDifficultyLevel())
                 .estimatedDays(route.getEstimatedDays())
                 .maxElevation(route.getMaxElevation())
                 .minElevation(route.getMinElevation())
                 .totalDistanceInKm(route.getDistanceInKm())
                 .timeStamp(route.getTimeStamp())
                 .build();

    }

    public RouteDetails toRouteDetails(@NonNull Route route)
    {
         return RouteDetails.builder()
                 .id(route.getId())
                 .name(route.getName())
                 .destinationId(route.getDestination().getId())
                 .description(route.getDescription())
                 .estimatedDays(route.getEstimatedDays())
                 .difficultyLevel(route.getDifficultyLevel())
                 .minElevation(route.getMinElevation())
                 .maxElevation(route.getMaxElevation())
                 .totalDistanceInKm(route.getDistanceInKm())
                 .routeStatus(route.getRouteStatus())
                 .build();


    }
    public NearbyRouteResponse toNearbyRouteResponse(@NonNull NearbyRouteProjection projections)
    {
        return NearbyRouteResponse.builder()
                .routeId(projections.getId())
                .routeName(projections.getName())
                .estimatedDays(projections.getEstimatedDays())
                .difficultyLevel(projections.getDifficultyLevel())
                .description(projections.getDescription())
                .routeDistanceInKm(projections.getDistanceInKm())
                .destinationName(projections.getDestinationName())
                .distanceToStartPoint(projections.getDistanceMeters()!=null || projections.getDistanceMeters()>1.0? projections.getDistanceMeters()/1000 : projections.getDistanceMeters())
                .build();
    }
}
