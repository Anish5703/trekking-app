package com.example.trekking_app.service;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.trackpoint.TrackPointResponse;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.exception.resource.ResourceDeletionFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.mapper.TrackPointMapper;
import com.example.trekking_app.model.TrackPointStatus;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TrackPointService {

    private final TrackPointRepository trackPointRepo;
    private final RouteRepository routeRepo;
    private final TrackPointMapper trackPointMapper;
    private IngestionOrchestratorService orchestratorService;

    public TrackPointService(TrackPointRepository trackPointRepo,
                             RouteRepository routeRepo, IngestionOrchestratorService orchestratorService) {
        this.trackPointRepo = trackPointRepo;
        this.routeRepo = routeRepo;
        this.trackPointMapper = new TrackPointMapper();
        this.orchestratorService = orchestratorService;
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<TrackPointResponse>> getAllTrackPoints(int routeId) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );

        List<TrackPoint> trackPointList = route.getTrackPoints().stream().sorted(Comparator.comparingInt(TrackPoint::getGlobalSequence)).toList();
        List<TrackPointResponse> trackPointResponseList = new ArrayList<>();
        trackPointList.forEach(trackPoint -> trackPointResponseList.add(trackPointMapper.toTrackPointResponse(trackPoint)));
        return new ApiResponse<>(trackPointResponseList, "trackpoints fetched", 200);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<TrackPointResponse>> getActiveTrackPoints(Integer routeId) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        List<TrackPoint> activeTrackPoints = trackPointRepo.findByRouteAndIsDeletedFalseOrderByGlobalSequenceAsc(route).orElseThrow(
                () -> new ResourceNotFoundException("active trackpoints", "route id", routeId)
        );
        List<TrackPointResponse> trackPointResponseList = new ArrayList<>();
        activeTrackPoints.forEach(tp -> trackPointResponseList.add(trackPointMapper.toTrackPointResponse(tp)));
        return new ApiResponse<>(trackPointResponseList, "active trackpoints fetched", 200);
    }


    @Transactional
    public ApiResponse<Void> deleteTrackPoint(Integer routeId, Integer trackPointId) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        try {
            TrackPoint trackPoint = trackPointRepo.findById(trackPointId).orElseThrow(
                    () -> new ResourceNotFoundException("trackpoint","id",trackPointId)
            );
            trackPoint.setIsDeleted(true);
            trackPoint.setStatus(TrackPointStatus.SOFT_DELETED);
            trackPointRepo.save(trackPoint);
            orchestratorService.mergeTrackPoints(routeId);
            return new ApiResponse<>(null, "trackpoint deleted", 200);
        } catch (Exception e) {
            throw new ResourceDeletionFailedException("trackpoint", "id", trackPointId);
        }
    }
}
