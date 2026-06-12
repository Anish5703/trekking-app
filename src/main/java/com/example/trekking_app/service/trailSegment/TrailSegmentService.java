package com.example.trekking_app.service.trailSegment;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.trailSegment.TrailSegmentResponse;
import com.example.trekking_app.dto.trailSegment.TrailSegmentUpdateRequest;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrailSegment;
import com.example.trekking_app.exception.resource.NoResourceFoundException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.mapper.TrailSegmentMapper;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrailSegmentRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrailSegmentService {


    private final TrailSegmentRepository trailSegmentRepo;
    private final RouteRepository routeRepo;
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
}
