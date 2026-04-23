package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.route.RouteRequest;
import com.example.trekking_app.dto.route.RouteResponse;
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
                 .userId(route.getUser().getId())
                 .estimatedDays(route.getEstimatedDays())
                 .timeStamp(route.getTimeStamp())
                 .build();

    }
}
