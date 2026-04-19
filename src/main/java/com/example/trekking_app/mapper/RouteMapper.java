package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.route.RouteRequest;
import com.example.trekking_app.dto.route.RouteResponse;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.User;
import lombok.NonNull;

public class RouteMapper {

    public Route toEntity(@NonNull RouteRequest routeRequest, User user)
    {
        return   Route.builder()
                .title(routeRequest.getTitle())
                .description(routeRequest.getDescription())
                .difficultyLevel(routeRequest.getDifficultyLevel())
                .user(user)
                .estimatedDays(routeRequest.getEstimatedDays())
                .district(routeRequest.getDistrict())
                .region(routeRequest.getRegion())
                .build();

    }

    public RouteResponse toRouteResponse(@NonNull Route route)
    {
         return RouteResponse.builder()
                 .id(route.getId())
                 .title(route.getTitle())
                 .description(route.getDescription())
                 .difficultyLevel(route.getDifficultyLevel())
                 .userId(route.getUser().getId())
                 .estimatedDays(route.getEstimatedDays())
                 .district(route.getDistrict())
                 .region(route.getRegion())
                 .timeStamp(route.getTimeStamp())
                 .build();

    }
}
