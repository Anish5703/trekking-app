package com.example.trekking_app.service.trackpoints;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.trackpoint.TrackPointRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointResponse;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.entity.TrailSegment;
import com.example.trekking_app.entity.WayPoint;
import com.example.trekking_app.exception.resource.ResourceDeletionFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.resource.ResourceUpdateFailedException;
import com.example.trekking_app.mapper.TrackPointMapper;
import com.example.trekking_app.model.RouteStatus;
import com.example.trekking_app.model.SegmentRefreshContext;
import com.example.trekking_app.model.TrackPointStatus;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import com.example.trekking_app.repository.TrailSegmentRepository;
import com.example.trekking_app.repository.WayPointRepository;
import com.example.trekking_app.service.gpx.GpxMergeService;
import com.example.trekking_app.service.trailSegment.TrailSegmentService;
import com.example.trekking_app.service.xlsx.XlsxParserHelper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackPointService {

    private final TrackPointRepository trackPointRepo;
    private final RouteRepository routeRepo;
    private final TrailSegmentService trailSegmentService;
    private final XlsxParserHelper trailSegmentHelper;
    private final TrailSegmentRepository trailSegmentRepo;
    private final TrackPointMapper trackPointMapper = new TrackPointMapper();
    private final GpxMergeService gpxMergeService;
    private final GeometryFactory GF = new GeometryFactory(new PrecisionModel(),4326);



    @Transactional(readOnly = true)
    public ApiResponse<Page<TrackPointResponse>> getAllTrackPoints(@NonNull Integer routeId ,
                                                                   @NonNull Integer page ,
                                                                   @NonNull Integer size) {
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
    public ApiResponse<Page<TrackPointResponse>> getActiveTrackPoints(@NonNull Integer routeId,
                                                                      Integer page ,
                                                                       Integer size) {
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

    @Transactional(readOnly = true)
    public ApiResponse<Page<TrackPointResponse>> getInactiveTrackPoints(@NonNull Integer routeId ,
                                                                        Integer page , Integer size)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        Pageable pageable = PageRequest.of(page,size);
        Page<TrackPointResponse> inactiveTrackPoints = trackPointRepo.findByRoute_IdAndIsDeletedTrueOrderByUpdatedAtAsc(routeId,pageable)
                .map(trackPointMapper::toTrackPointResponse);
        String message = inactiveTrackPoints.isEmpty()
                ? "no deleted trackpoints found  "
                : "deleted trackpoints fetched";
        return new ApiResponse<>(inactiveTrackPoints,message,200);
    }

    @Deprecated(since = "version 2.0")
    public ApiResponse<TrackPointResponse> updateTrackPoint_V1(@NonNull Integer routeId , @NonNull Integer trackPointId , @NonNull TrackPointRequest trackPointRequest)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        TrackPoint trackPoint = trackPointRepo.findByIdAndRoute_Id(trackPointId,routeId).orElseThrow(
                () -> new ResourceNotFoundException("trackpoint","id",trackPointId)
        );
        if(route.getRouteStatus().equals(RouteStatus.MERGING)) throw new ResourceUpdateFailedException("failed to update trackpoint since the associated routes trackpoints are merging");
        try {
            trackPoint = trackPointMapper.toUpdateTrackPoint(trackPoint,trackPointRequest);
            trackPointRepo.save(trackPoint);
            TrackPointResponse tpResponse = trackPointMapper.toTrackPointResponse(trackPoint);
             gpxMergeService.mergeTrackPoints(routeId);
            return new ApiResponse<>(tpResponse, "trackpoint updated", 200);
        }
        catch (Exception e)
        {
            log.error("Trackpoint update failed : {}",e.getLocalizedMessage());
            throw new ResourceUpdateFailedException("trackpoint","id",trackPointId);
        }

    }

    @Transactional
    public ApiResponse<TrackPointResponse> updateTrackPoint(@NonNull Integer routeId,
                                                            @NonNull Integer trackPointId,
                                                            @NonNull TrackPointRequest trackPointRequest) {
        Route route = routeRepo.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("route", "id", routeId));
        TrackPoint trackPoint = trackPointRepo.findByIdAndRoute_Id(trackPointId, routeId)
                .orElseThrow(() -> new ResourceNotFoundException("trackpoint", "id", trackPointId));

        if (route.getRouteStatus().equals(RouteStatus.MERGING))
            throw new ResourceUpdateFailedException(
                    "failed to update trackpoint since the associated route's trackpoints are merging");

        try {
            // Step 1 — resolve affected segments BEFORE coords/sequence change
            List<SegmentRefreshContext> contexts = trailSegmentService.resolveAffectedSegments(routeId, trackPoint);

            // Step 2 — update trackpoint and reassign global sequences
            trackPoint = trackPointMapper.toUpdateTrackPoint(trackPoint, trackPointRequest);
            trackPointRepo.save(trackPoint);
            TrackPointResponse tpResponse = trackPointMapper.toTrackPointResponse(trackPoint);
            gpxMergeService.mergeTrackPoints(routeId);

            // Step 3 — refresh affected segments using pre-resolved trackpoint IDs
            if (!contexts.isEmpty()) {
                List<TrailSegment> toSave = new ArrayList<>();
                for (SegmentRefreshContext ctx : contexts) {
                    // re-fetch by ID to get updated globalSequence post-merge
                    TrackPoint startTp = trackPointRepo.findById(ctx.startTp().getId()).orElse(ctx.startTp());
                    TrackPoint endTp = trackPointRepo.findById(ctx.endTp().getId()).orElse(ctx.endTp());

                    LineString newPath = trailSegmentHelper.generateTrailSegmentPath(startTp, endTp, route);
                    ctx.segment().setPath(newPath);
                    toSave.add(ctx.segment());
                }
                trailSegmentRepo.saveAll(toSave);
                log.info("Refreshed {} trail segments after trackpoint {} update", toSave.size(), trackPointId);
            }

            return new ApiResponse<>(tpResponse, "trackpoint updated", 200);

        } catch (Exception e) {
            log.error("Trackpoint update failed : {}", e.getLocalizedMessage());
            throw new ResourceUpdateFailedException("trackpoint", "id", trackPointId);
        }

    }


   @Transactional
    public ApiResponse<Void> deleteTrackPoint(Integer routeId, Integer trackPointId) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
            TrackPoint trackPoint = trackPointRepo.findById(trackPointId).orElseThrow(
                    () -> new ResourceNotFoundException("trackpoint","id",trackPointId)
            );
            try{
            trackPoint.setIsDeleted(true);
            trackPoint.setStatus(TrackPointStatus.SOFT_DELETED);
            trackPointRepo.save(trackPoint);
            gpxMergeService.mergeTrackPoints(route.getId());
            return new ApiResponse<>(null, "trackpoint deleted", 200);
        } catch (Exception e) {
            throw new ResourceDeletionFailedException("trackpoint", "id", trackPointId);
        }
    }

    @Transactional
    public ApiResponse<Void> recoverTrackPoint(@NonNull Integer routeId, @NonNull Integer trackPointId)
    {
        TrackPoint tp = trackPointRepo.findByIdAndRoute_Id(trackPointId,routeId).orElseThrow(
                () -> new ResourceNotFoundException("trackpoint","route id and trackpoint id",String.format("%d and %d respectively",routeId,trackPointId))
        );
        if(!tp.getIsDeleted() || !tp.getStatus().equals(TrackPointStatus.SOFT_DELETED))
            throw new ResourceUpdateFailedException("trackpoint recovery failed ! it was never deleted");
        tp.setStatus(TrackPointStatus.RECOVERED);
        tp.setIsDeleted(false);
        trackPointRepo.save(tp);
        return new ApiResponse<>(null,"trackpoint recovered",200);


    }
}
