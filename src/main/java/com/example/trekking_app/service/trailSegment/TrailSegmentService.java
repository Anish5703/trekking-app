package com.example.trekking_app.service.trailSegment;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.trailSegment.TrailSegmentResponse;
import com.example.trekking_app.dto.trailSegment.TrailSegmentUpdateRequest;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.entity.TrailSegment;
import com.example.trekking_app.entity.WayPoint;
import com.example.trekking_app.exception.resource.NoResourceFoundException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.mapper.TrailSegmentMapper;
import com.example.trekking_app.model.SegmentRefreshContext;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import com.example.trekking_app.repository.TrailSegmentRepository;
import com.example.trekking_app.repository.WayPointRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class TrailSegmentService {


    private final TrailSegmentRepository trailSegmentRepo;
    private final RouteRepository routeRepo;
    private final TrackPointRepository trackPointRepo;
    private final TrailSegmentMapper trailSegmentMapper =new TrailSegmentMapper();

    @Transactional(readOnly = true)
    public ApiResponse<Page<TrailSegmentResponse>> getAllTrailSegments(@NonNull Integer routeId,Integer page,
                                                                       Integer size)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        Pageable pageable = PageRequest.of(page,size);
        Page<TrailSegmentResponse> trailResponses= trailSegmentRepo.findByRoute_Id(route.getId(),pageable)
                .map(trailSegmentMapper::toTrailSegmentResponse);
        if(trailResponses.isEmpty()) throw new NoResourceFoundException("trail segments for route "+routeId);

        return new ApiResponse<>(trailResponses,"trail segments fetched",200);
    }

    @Transactional(readOnly = true)
    public ApiResponse<TrailSegmentResponse> getTrailSegment(@NonNull Integer routeId,@NonNull Integer trailSegmentId)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        TrailSegment trailSegment = trailSegmentRepo.findByIdAndRoute_Id(trailSegmentId,routeId).orElseThrow(
                () -> new ResourceNotFoundException("trail segment","id and route id",String.format("%d and %d respectively",trailSegmentId,routeId))
        );
        TrailSegmentResponse trailResponse = trailSegmentMapper.toTrailSegmentResponse(trailSegment);
        return new ApiResponse<>(trailResponse,"trail segment fetched",200);
    }

    @Transactional
    public ApiResponse<TrailSegmentResponse> updateTrailSegment(@NonNull Integer routeId, @NonNull Integer trailSegmentId,
                                                                @NonNull TrailSegmentUpdateRequest trailRequest)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        TrailSegment trailSegment = trailSegmentRepo.findByIdAndRoute_Id(trailSegmentId,routeId).orElseThrow(
                () -> new ResourceNotFoundException("trail segment","id and route id",String.format("%d and %d respectively",trailSegmentId,routeId))
        );
        TrailSegment updatedTrail = trailSegmentMapper.toUpdateEntity(trailSegment,trailRequest);
        updatedTrail = trailSegmentRepo.save(updatedTrail);
        TrailSegmentResponse trailResponse = trailSegmentMapper.toTrailSegmentResponse(updatedTrail);
        return new ApiResponse<>(trailResponse,"trail segment updated",200);
    }
    public List<SegmentRefreshContext> resolveAffectedSegments(Integer routeId, TrackPoint trackPoint) {
        Integer updatedSeq = trackPoint.getGlobalSequence();
        if (updatedSeq == null) {
            log.warn("TrackPoint {} has no globalSequence, skipping segment refresh", trackPoint.getId());
            return Collections.emptyList();
        }

        List<TrailSegment> allSegments = trailSegmentRepo.findByRoute_Id(routeId);
        if (allSegments.isEmpty()) return Collections.emptyList();

        List<SegmentRefreshContext> contexts = new ArrayList<>();

        for (TrailSegment seg : allSegments) {
            WayPoint sw = seg.getStartWaypoint();
            WayPoint ew = seg.getEndWaypoint();

            Optional<TrackPoint> startTpOpt = trackPointRepo
                    .findByRoute_IdAndLatitudeAndLongitude(routeId, sw.getLatitude(), sw.getLongitude());
            Optional<TrackPoint> endTpOpt = trackPointRepo
                    .findByRoute_IdAndLatitudeAndLongitude(routeId, ew.getLatitude(), ew.getLongitude());

            if (startTpOpt.isEmpty() || endTpOpt.isEmpty()) {
                log.warn("Could not resolve boundary trackpoints for segment {}, skipping", seg.getId());
                continue;
            }

            TrackPoint startTp = startTpOpt.get();
            TrackPoint endTp   = endTpOpt.get();

            if (startTp.getGlobalSequence() == null || endTp.getGlobalSequence() == null) continue;

            int low  = Math.min(startTp.getGlobalSequence(), endTp.getGlobalSequence());
            int high = Math.max(startTp.getGlobalSequence(), endTp.getGlobalSequence());

            if (updatedSeq >= low && updatedSeq <= high) {
                log.debug("Segment {} affected by trackpoint {} update (seq {} in range [{},{}])",
                        seg.getId(), trackPoint.getId(), updatedSeq, low, high);
                contexts.add(new SegmentRefreshContext(seg, startTp, endTp));
            }
        }

        return contexts;
    }

}
