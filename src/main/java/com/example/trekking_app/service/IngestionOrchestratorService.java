package com.example.trekking_app.service;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.gpx.GpxImportResponse;
import com.example.trekking_app.dto.gpx.GpxSegmentOrderRequest;
import com.example.trekking_app.dto.gpx.GpxSegmentResponse;
import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.exception.resource.NoResourceFoundException;
import com.example.trekking_app.exception.resource.ResourceDeletionFailedException;
import com.example.trekking_app.exception.resource.ResourceMergeFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.mapper.GpxSegmentMapper;
import com.example.trekking_app.repository.GpxSegmentRepository;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionOrchestratorService {

    private final RouteRepository routeRepo;
    private final GpxSegmentRepository gpxSegmentRepo;
    private final GpxParserService gpxParserService;
    private final TrackPointRepository trackPointRepo;
    private final GpxMergeService gpxMergeService;
    private final GpxSegmentMapper gpxSegmentMapper = new GpxSegmentMapper();


    @Transactional
    public ApiResponse<List<GpxImportResponse>> uploadGpxFiles(@NonNull Integer routeId , List<MultipartFile> files) throws IOException {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        int nextOrder = gpxSegmentRepo.findByRoute_IdOrderByOrderIndexAsc(routeId).stream().mapToInt(GpxSegment::getOrderIndex).max().orElse(0)+1;
        List<GpxImportResponse> gpxImportResponses = new ArrayList<>();
        for(MultipartFile file : files)
        {
          GpxImportResponse gpxImportResponse =  gpxParserService.parse(file,route,nextOrder);
           nextOrder++;
           gpxImportResponses.add(gpxImportResponse);
        }
        gpxMergeService.mergeTrackPoints(routeId);
        return new ApiResponse<>(gpxImportResponses,"gpx file uploaded",201);

    }

    @Transactional(readOnly = true)
    public ApiResponse<List<GpxSegmentResponse>> getAllGpxSegment(@NonNull Integer routeId)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );

        List<GpxSegment> gpxSegmentList = gpxSegmentRepo.findByRoute(route).orElseThrow(
                () -> new ResourceNotFoundException("gpx segments","route id",routeId) );

          List<GpxSegmentResponse> gpxSegmentResponseList = new ArrayList<>();

          gpxSegmentList.forEach(gpxSegment ->
                  gpxSegmentResponseList.add(gpxSegmentMapper.toGpxSegmentResponse(gpxSegment)
                  ));

          return new ApiResponse<>(gpxSegmentResponseList,"gpx segments fetched",200);

    }

    public ApiResponse<Void> reorderGpxSegment(@NonNull GpxSegmentOrderRequest segmentOrderRequest , @NonNull Integer routeId)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        List<GpxSegment> gpxSegments = gpxSegmentRepo.findByRoute(route).orElseThrow(
                () -> new ResourceNotFoundException("gpx segments","route id",routeId)
        );
        gpxSegments.forEach(gpxSegment -> gpxSegment.setOrderIndex(segmentOrderRequest.getSegmentIdWithOrder().get(gpxSegment.getId())));
        gpxSegmentRepo.saveAll(gpxSegments);
        gpxMergeService.mergeTrackPoints(routeId);
        return new ApiResponse<>(null,"gpx segments reordered",200);
    }

    @Transactional
    public ApiResponse<Void> deleteGpxSegment(@NonNull Integer gpxSegmentId,@NonNull Integer routeId)
    {
        GpxSegment gpxSegment = gpxSegmentRepo.findById(gpxSegmentId).orElseThrow(
                () -> new ResourceNotFoundException("gpx segment","id",gpxSegmentId)
        );
        try {
            trackPointRepo.deleteAllByGpxSegment_Id(gpxSegment.getId());
            gpxSegmentRepo.deleteById(gpxSegment.getId());
            gpxMergeService.mergeTrackPoints(routeId);
            return new ApiResponse<>(null,"gpx segment deleted",200);
        }
        catch (Exception e)
        {
            throw new ResourceDeletionFailedException("gpx segment","id",gpxSegmentId);
        }
    }



}
