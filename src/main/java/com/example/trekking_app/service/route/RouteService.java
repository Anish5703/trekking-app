package com.example.trekking_app.service.route;

import com.example.trekking_app.dto.geoJson.GeoJsonFeatureCollection;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.*;
import com.example.trekking_app.entity.*;
import com.example.trekking_app.exception.auth.UserNotFoundException;
import com.example.trekking_app.exception.resource.NoResourceFoundException;
import com.example.trekking_app.exception.resource.ResourceDeletionFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.route.CreateRouteFailedException;
import com.example.trekking_app.mapper.GeoJsonMapper;
import com.example.trekking_app.mapper.RouteMapper;
import com.example.trekking_app.model.RouteStatus;
import com.example.trekking_app.repository.*;
import jakarta.annotation.Nonnull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sound.midi.Track;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private final RecentlyViewedRepository recentlyViewedRepo;
    private final TrackPointRepository trackPointRepo;



    @Transactional(readOnly = true)
    public ApiResponse<RouteResponse> getRoute(Integer routeId) {
        if (routeId < 1) throw new IllegalArgumentException("invalid route id ");

        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)

        );
        TrackPoint tp1 = trackPointRepo.findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceAsc(route.getId()).orElse(null);
        TrackPoint tp2 = trackPointRepo.findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceDesc(route.getId()).orElse(null);
        Point startCoords = tp1!=null ? tp1.getGeom() : null;
        Point endCoords = tp2!=null ? tp2.getGeom() : null;
        RouteResponse routeResponse = routeMapper.toRouteResponse(route,startCoords,endCoords);
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
                () -> new ResourceNotFoundException("destination","id", routeRequest.getDestinationId())
        );

        Route route = routeMapper.toEntity(routeRequest, user, destination);
        try {
            Route newRoute = routeRepo.save(route);
            RouteResponse routeResponse = routeMapper.toRouteResponse(newRoute,null,null);
            return new ApiResponse<>(routeResponse, "New route created", 201);
        } catch (Exception ex) {
            if(routeRepo.existsByNameAndDestination_Id(routeRequest.getName(),destination.getId())) throw new CreateRouteFailedException("route with this name and destination already exists");
            log.error("Failed to create new route : {}", ex.getLocalizedMessage());
            throw new CreateRouteFailedException("Failed to create new route");
        }
    }

    @Cacheable(value="route-geoJson",key ="#routeId + ':' + #tolerance" )
    @Transactional(readOnly = true)
    public GeoJsonFeatureCollection getRouteGeoJson(@NonNull Integer routeId,@Nonnull Double tolerance) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        log.info("fetching geoJson for route {}",routeId);
        if(route.getPath()==null) throw new CreateRouteFailedException("no path created for this route");
        switch (route.getRouteStatus()) {
            case IDLE ->
                    throw new ResourceNotFoundException("No path available — route has no merged track points yet");
            case MERGING ->
                    throw new ResourceNotFoundException("Route is currently merging — please try again shortly");
            case FAILED ->
                    throw new ResourceNotFoundException("Route merge failed — please re-trigger the merge");
            case MERGED -> { }
            default ->
                    throw new IllegalStateException("Unexpected route status: " + route.getRouteStatus());
        }
        try {
            return geoJsonMapper.toGeoJson(route,tolerance);

        } catch (Exception e) {
            log.error("failed to convert route path to geo json : {}", e.getLocalizedMessage());
            throw new CreateRouteFailedException("failed to create route path");
        }
    }
    @CacheEvict(value="route-geoJson",key = "#routeId" , allEntries = true)
   @Transactional
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
        newRoute.setMinElevation(route.getMinElevation());
        newRoute.setMaxElevation(route.getMaxElevation());
        newRoute.setDistanceInKm(route.getDistanceInKm());
        newRoute.setId(route.getId());
        Route updatedRoute = routeRepo.save(newRoute);
        TrackPoint tp1 = trackPointRepo.findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceAsc(route.getId()).orElse(null);
        TrackPoint tp2 = trackPointRepo.findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceDesc(route.getId()).orElse(null);
        Point startCoords = tp1!=null ? tp1.getGeom() : null;
        Point endCoords = tp2!=null ? tp2.getGeom() : null;

        RouteResponse routeResponse = routeMapper.toRouteResponse(updatedRoute,startCoords,endCoords);
        return new ApiResponse<>(routeResponse,"route updated",200);
    }
    @CacheEvict(value = "route-geoJson" ,key="#routeId" , allEntries = true)
   @Transactional
    public ApiResponse<Void> deleteRoute(@NonNull Integer routeId)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        if(!(route.getRouteStatus() ==null))
        {
            if (route.getRouteStatus().equals(RouteStatus.MERGING))
                throw new ResourceDeletionFailedException("failed to delete route since it is merging currently");
        }
        Optional<List<GpxSegment>> gpxSegments = gpxSegmentRepo.findByRoute(route);
        if(gpxSegments.isPresent() && !gpxSegments.get().isEmpty())
            throw new ResourceDeletionFailedException("failed to delete route ! delete gpx files associated with route first");
        routeRepo.delete(route);
        return new ApiResponse<>(null,"route deleted",200);

    }

    @Transactional(readOnly = true)
    public ApiResponse<List<NearbyRouteResponse>> getNearbyRoutes(NearbyRouteRequest routeRequest)
    {
        List<NearbyRouteProjection> nearbyRoutes = routeRepo.findNearbyRoutes(routeRequest.getLatitude(),
                routeRequest.getLongitude(),
                routeRequest.getRadiusMeters(),routeRequest.getLimit());
        if(nearbyRoutes.isEmpty()) throw new NoResourceFoundException("nearby routes");
        List<NearbyRouteResponse> routeResponses = nearbyRoutes.stream().map(routeMapper::toNearbyRouteResponse).toList();
        return new ApiResponse<>(routeResponses,"nearby route fetched",200);
    }

    public ApiResponse<Page<RouteDetails>> getRecentlyViewedRoutes(@NonNull Integer userId , @NonNull Integer page, @NonNull Integer size)
    {
        User user = userRepo.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("user","id",userId)
        );

        Pageable pageable = PageRequest.of(page,size);
        Page<RouteDetails> recentlyViewedRoutes = recentlyViewedRepo.findByUser_IdOrderByUpdatedAtDesc(user.getId(),pageable).
                map(rc -> routeMapper.toRouteDetails(rc.getRoute()));
        if(recentlyViewedRoutes.isEmpty()) throw new NoResourceFoundException("recently viewed routes");
        return new ApiResponse<>(recentlyViewedRoutes,"recently viewed routes fetched",200);
    }

    public ApiResponse<Page<RouteDetails>> getPopularRoutes(@NonNull Integer page, @NonNull Integer size)
    {
        Pageable pageable = PageRequest.of(page,size);
        Page<RouteDetails> popularRoutes = recentlyViewedRepo.findMostPopularRoutes(pageable).map(routeMapper::toRouteDetails);
        if(popularRoutes.isEmpty()) throw new NoResourceFoundException("popular routes");
        return new ApiResponse<>(popularRoutes,"popular routes fetched",200);

    }

    public void updateRecentlyViewedStatus(@NonNull  Integer userId,@NonNull Integer routeId)
    {
        User user = userRepo.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("user","id",userId)
        );
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        RecentlyViewed rv = recentlyViewedRepo.findByUser_IdAndRoute_Id(user.getId(),route.getId()).orElse(null);
        if(rv!=null)
        rv.setCounter(rv.getCounter()+1);
        else
        {
            rv = RecentlyViewed.builder().
                    user(user).route(route).counter(1).build();
        }
        recentlyViewedRepo.save(rv);
        log.info("updated recently view data");
    }

    public ApiResponse<Page<RouteDetails>> searchRoutesByKeyword(@NonNull String keyword, @NonNull Integer page, @NonNull Integer size)
    {
        Pageable pageable = PageRequest.of(page,size);
        Page<RouteDetails> foundRoutes = routeRepo.searchByKeyword(keyword,pageable).map(routeMapper::toRouteDetails);
        if(foundRoutes.isEmpty()) throw new ResourceNotFoundException("route","keyword",keyword);
        return new ApiResponse<>(foundRoutes,"searched results",200);
    }
}

