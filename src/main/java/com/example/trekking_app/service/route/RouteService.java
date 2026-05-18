package com.example.trekking_app.service.route;

import com.example.trekking_app.dto.geoJson.GeoJsonFeature;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.RouteDetails;
import com.example.trekking_app.dto.route.RouteRequest;
import com.example.trekking_app.dto.route.RouteResponse;
import com.example.trekking_app.entity.*;
import com.example.trekking_app.exception.auth.UserNotFoundException;
import com.example.trekking_app.exception.resource.NoResourceFoundException;
import com.example.trekking_app.exception.resource.ResourceDeletionFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.route.CreateRouteFailedException;
import com.example.trekking_app.exception.destination.DestinationNotFoundException;
import com.example.trekking_app.mapper.GeoJsonMapper;
import com.example.trekking_app.mapper.RouteMapper;
import com.example.trekking_app.repository.DestinationRepository;
import com.example.trekking_app.repository.GpxSegmentRepository;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepo;
    private final DestinationRepository destinationRepo;
    private final UserRepository userRepo;
    private final RouteMapper routeMapper = new RouteMapper();
    private final GeoJsonMapper geoJsonMapper = new GeoJsonMapper();
    private final GpxSegmentRepository gpxSegmentRepo;


    @Transactional(readOnly = true)
    public ApiResponse<RouteResponse> getRoute(Integer routeId) {
        if (routeId < 1) throw new IllegalArgumentException("invalid route id ");

        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)

        );
        RouteResponse routeResponse = routeMapper.toRouteResponse(route);
        return new ApiResponse<>(routeResponse, "route fetched", 200);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<RouteDetails>> getAllDestinationRoutes(Integer destinationId) {
        if (destinationId < 1) throw new IllegalArgumentException("invalid destination id");

        Destination destination = destinationRepo.findById(destinationId).orElseThrow(
                () -> new ResourceNotFoundException("destination", "id", destinationId)
        );
        List<Route> routeList = routeRepo.findAllByDestination_Id(destination.getId()).orElseThrow(
                () -> new NoResourceFoundException("routes")
        );
        List<RouteDetails> routeResponseList = new ArrayList<>();
        routeList.forEach(
                route -> routeResponseList.add(routeMapper.toRouteDetails(route))
        );
        return new ApiResponse<>(routeResponseList, "routes fetched ", 200);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Page<RouteDetails>> getAllRoutes(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        if (routeRepo.count() < 1) throw new NoResourceFoundException("routes");
        Page<RouteDetails> routeDetails = routeRepo.findAll(pageable).map(routeMapper::toRouteDetails);
        return new ApiResponse<>(routeDetails, "routes fetched", 200);
    }

    @Transactional
    public ApiResponse<RouteResponse> createRoute(RouteRequest routeRequest, Integer userId) {
        User user = userRepo.findById(userId).orElseThrow(
                () -> new UserNotFoundException("No user found with id " + userId)
        );
        Destination destination = destinationRepo.findById(routeRequest.getDestinationId()).orElseThrow(
                () -> new DestinationNotFoundException("No destination found with id " + routeRequest.getDestinationId())
        );

        Route route = routeMapper.toEntity(routeRequest, user, destination);
        try {
            Route newRoute = routeRepo.save(route);
            RouteResponse routeResponse = routeMapper.toRouteResponse(newRoute);
            return new ApiResponse<>(routeResponse, "New route created", 201);
        } catch (Exception ex) {
            log.error("Failed to create new route : {}", ex.getLocalizedMessage());
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

    public ApiResponse<RouteResponse> updateRoute(@NonNull Integer routeId, @NonNull RouteRequest routeRequest,@NonNull Integer userId) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        Destination destination = destinationRepo.findById(routeRequest.getDestinationId()).orElseThrow(
                () -> new ResourceNotFoundException("destination","id",routeRequest.getDestinationId())
        );
        User user = userRepo.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("user","id",userId)
        );
        Route newRoute = routeMapper.toEntity(routeRequest,user,destination);
        if(newRoute.getMinElevation().isNaN()) newRoute.setMinElevation(route.getMinElevation());
        if(newRoute.getMaxElevation().isNaN()) newRoute.setMaxElevation(route.getMaxElevation());
        if(newRoute.getDistanceInKm()<0.1) newRoute.setDistanceInKm(route.getDistanceInKm());
        newRoute.setId(route.getId());
        Route updatedRoute = routeRepo.save(newRoute);
        RouteResponse routeResponse = routeMapper.toRouteResponse(updatedRoute);
        return new ApiResponse<>(routeResponse,"route updated",200);
    }

    public ApiResponse<Void> deleteRoute(@NonNull Integer routeId)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        Optional<List<GpxSegment>> gpxSegments = gpxSegmentRepo.findByRoute(route);
        if(gpxSegments.isPresent() && !gpxSegments.get().isEmpty())
            throw new ResourceDeletionFailedException("failed to delete route ! delete gpx files associated with route first");
        routeRepo.delete(route);
        return new ApiResponse<>(null,"route deleted",200);

    }

}

