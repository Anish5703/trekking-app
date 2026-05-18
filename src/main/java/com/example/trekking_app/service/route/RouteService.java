package com.example.trekking_app.service;

import com.example.trekking_app.dto.geoJson.GeoJsonFeature;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.RouteDetails;
import com.example.trekking_app.dto.route.RouteRequest;
import com.example.trekking_app.dto.route.RouteResponse;
import com.example.trekking_app.entity.Destination;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.exception.auth.UserNotFoundException;
import com.example.trekking_app.exception.resource.NoResourceFoundException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.route.CreateRouteFailedException;
import com.example.trekking_app.exception.destination.DestinationNotFoundException;
import com.example.trekking_app.mapper.GeoJsonMapper;
import com.example.trekking_app.mapper.RouteMapper;
import com.example.trekking_app.repository.DestinationRepository;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepo;
    private final DestinationRepository destinationRepo;
    private final UserRepository userRepo;
    private final RouteMapper routeMapper = new RouteMapper();
    private final GeoJsonMapper geoJsonMapper = new GeoJsonMapper();


    @Transactional(readOnly = true )
    public ApiResponse<RouteResponse> getRoute(Integer routeId)
    {
        if(routeId < 1) throw new IllegalArgumentException("invalid route id ");

        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)

        );
        RouteResponse routeResponse = routeMapper.toRouteResponse(route);
        return new ApiResponse<>(routeResponse,"route fetched",200);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<RouteDetails>> getAllRoute(Integer destinationId)
    {
        if(destinationId < 1) throw new IllegalArgumentException("invalid destination id");

        Destination destination = destinationRepo.findById(destinationId).orElseThrow(
                () -> new ResourceNotFoundException("destination","id",destinationId)
        );
        List<Route> routeList = routeRepo.findAllByDestination_Id(destination.getId()).orElseThrow(
                () -> new NoResourceFoundException("routes")
        );
        List<RouteDetails> routeResponseList = new ArrayList<>();
        routeList.forEach(
                route -> routeResponseList.add(routeMapper.toRouteDetails(route))
        );
        return new ApiResponse<>(routeResponseList,"routes fetched ",200);
    }

    @Transactional
    public ApiResponse<RouteResponse> createRoute(RouteRequest routeRequest,Integer userId)
    {
      User user = userRepo.findById(userId).orElseThrow(
              () -> new UserNotFoundException("No user found with id "+userId)
      );
        Destination destination = destinationRepo.findById(routeRequest.getDestinationId()).orElseThrow(
                () -> new DestinationNotFoundException("No destination found with id "+routeRequest.getDestinationId())
        );

        Route route = routeMapper.toEntity(routeRequest,user,destination);
      try{
         Route newRoute =  routeRepo.save(route);
         RouteResponse routeResponse = routeMapper.toRouteResponse(newRoute);
         return new ApiResponse<>(routeResponse,"New route created",201);
      }
      catch(Exception ex)
      {
          log.error("Failed to create new route : {}",ex.getLocalizedMessage());
          throw new CreateRouteFailedException("Failed to create new route");
      }
    }

    @Transactional(readOnly = true)
    public ApiResponse<GeoJsonFeature> getRoutePath(@NonNull Integer routeId) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        try {
            GeoJsonFeature feature = geoJsonMapper.toGeoJson(route);
            return new ApiResponse<>(feature, "route path fetched", 200);
        } catch (Exception e) {
            log.error("failed to convert route path to geojson : {}", e.getLocalizedMessage());
            throw new CreateRouteFailedException("failed to create route path");
        }
    }
}
