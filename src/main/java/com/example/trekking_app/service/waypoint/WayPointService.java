package com.example.trekking_app.service.waypoint;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.waypoint.WayPointResponse;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.WayPoint;
import com.example.trekking_app.exception.resource.ResourceDeletionFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.mapper.WayPointMapper;
import com.example.trekking_app.model.WayPointStatus;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.WayPointRepository;
import com.example.trekking_app.service.trackpoints.GpxMergeService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WayPointService {

    private final WayPointRepository wayPointRepo;
    private final RouteRepository routeRepo;
    private final WayPointMapper wayPointMapper = new WayPointMapper();
    private final GpxMergeService gpxMergeService;

    @Transactional(readOnly = true)
    public ApiResponse<Page<WayPointResponse>> getAllWayPoints(@NonNull Integer routeId ,
                                                               @NonNull Integer page ,
                                                               @NonNull Integer size) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        Pageable pageable = PageRequest.of(page, size);
        Page<WayPointResponse> wayPoints = wayPointRepo.findByRoute_IdOrderByGlobalSequenceAsc(route.getId(),pageable).map(wayPointMapper::toWayPointResponse);
        String message = wayPoints.isEmpty()
                ? "no waypoints found for route"
                : "waypoints fetched";

        return new ApiResponse<>(wayPoints, message, 200);
    }


    @Transactional(readOnly = true)
    public ApiResponse<Page<WayPointResponse>> getActiveWayPoints(Integer routeId, @NonNull Integer page, @NonNull Integer size) {

        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        Pageable pageable = PageRequest.of(page,size);
        Page<WayPointResponse> activeTrackPoints = wayPointRepo.findByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceAsc(route.getId(),pageable)
                .map(wayPointMapper::toWayPointResponse);
        String message = activeTrackPoints.isEmpty()
                ? "no active waypoints found for route"
                : "active waypoints fetched";
        return new ApiResponse<>(activeTrackPoints, message, 200);
    }

@Transactional(readOnly = true)
    public ApiResponse<Page<WayPointResponse>> getInactiveWayPoints(Integer routeId, @NonNull Integer page, @NonNull Integer size) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        Pageable pageable = PageRequest.of(page,size);
        Page<WayPointResponse> inactiveWayPoints = wayPointRepo.findByRoute_IdAndIsDeletedTrueOrderByUpdatedAtAsc(routeId,pageable)
                .map(wayPointMapper::toWayPointResponse);
        String message = inactiveWayPoints.isEmpty()
                ? "no deleted waypoints found  "
                : "deleted waypoints fetched";
        return new ApiResponse<>(inactiveWayPoints,message,200);
    }

@Transactional
    public ApiResponse<Void> deleteWayPoint(Integer routeId, @NonNull Integer wayPointId)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        try {
            WayPoint wayPoint = wayPointRepo.findById(wayPointId).orElseThrow(
                    () -> new ResourceNotFoundException("waypoint","id",wayPointId)
            );
            wayPoint.setIsDeleted(true);
            wayPoint.setStatus(WayPointStatus.SOFT_DELETED);
            wayPointRepo.save(wayPoint);
            gpxMergeService.mergeWayPoints(route.getId());
            return new ApiResponse<>(null, "waypoint deleted", 200);
        } catch (Exception e) {
            throw new ResourceDeletionFailedException("waypoint", "id", wayPointId);
        }
    }
}
