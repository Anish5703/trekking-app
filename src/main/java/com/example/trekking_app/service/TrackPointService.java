package com.example.trekking_app.service;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.trackpoint.TrackPointResponse;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.exception.resource.ResourceDeletionFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.mapper.GpxSegmentMapper;
import com.example.trekking_app.mapper.TrackPointMapper;
import com.example.trekking_app.model.TrackPointStatus;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrackPointService {

    private final TrackPointRepository trackPointRepo;
    private final RouteRepository routeRepo;
    private final TrackPointMapper trackPointMapper = new TrackPointMapper();
    private final GpxMergeService gpxMergeService;



    @Transactional(readOnly = true)
    public ApiResponse<Page<TrackPointResponse>> getAllTrackPoints(int routeId , int page ,int size) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        Pageable pageable = PageRequest.of(page, size);
        Page<TrackPointResponse> trackPointResponses = trackPointRepo.findByRoute_IdOrderByGlobalSequenceAsc(routeId,pageable).map(trackPointMapper::toTrackPointResponse);
        String message = trackPointResponses.isEmpty()
                ? "no trackpoints found for route"
                : "trackpoints fetched";

        return new ApiResponse<>(trackPointResponses, message, 200);
    }

    @Transactional(readOnly = true)
    public ApiResponse<Page<TrackPointResponse>> getActiveTrackPoints(Integer routeId, Integer page , Integer size) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        Pageable pageable = PageRequest.of(page,size);
        Page<TrackPointResponse> activeTrackPoints = trackPointRepo.findByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceAsc(routeId,pageable)
                .map(trackPointMapper::toTrackPointResponse);
        String message = activeTrackPoints.isEmpty()
                ? "no active trackpoints found for route"
                : "active trackpoints fetched";
        return new ApiResponse<>(activeTrackPoints, message, 200);
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
            gpxMergeService.mergeTrackPoints(routeId);
            return new ApiResponse<>(null, "trackpoint deleted", 200);
        } catch (Exception e) {
            throw new ResourceDeletionFailedException("trackpoint", "id", trackPointId);
        }
    }
}
