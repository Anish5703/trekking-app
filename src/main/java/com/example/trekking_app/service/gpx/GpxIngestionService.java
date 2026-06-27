package com.example.trekking_app.service.gpx;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.gpx.GpxImportResponse;
import com.example.trekking_app.dto.gpx.GpxSegmentOrderRequest;
import com.example.trekking_app.dto.gpx.GpxSegmentResponse;
import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.exception.resource.*;
import com.example.trekking_app.mapper.GpxSegmentMapper;
import com.example.trekking_app.model.GpxSegmentStatus;
import com.example.trekking_app.repository.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GpxIngestionService {

    private final RouteRepository routeRepo;
    private final GpxSegmentRepository gpxSegmentRepo;
    private final GpxParserService gpxParserService;
    private final TrackPointRepository trackPointRepo;
    private final WayPointRepository wayPointRepo;
    private final TrailSegmentRepository trailSegmentRepo;
    private final POIRepository poiRepo;
    private final AccommodationRepository accommodationRepo;
    private final GpxMergeService gpxMergeService;
    private final GpxSegmentMapper gpxSegmentMapper = new GpxSegmentMapper();

    @CacheEvict(value = "route-geoJson" , key = "#routeId" , allEntries = true)
    @Transactional
    public ApiResponse<List<GpxImportResponse>> uploadGpxFiles(@NonNull Integer routeId , List<MultipartFile> files ,@NonNull GpxSegmentStatus segmentStatus) throws IOException {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        int nextOrder =  gpxSegmentRepo.findTopByRoute_IdAndSegmentStatusOrderByOrderIndexDesc(routeId, segmentStatus)
                .map(GpxSegment::getOrderIndex)
                .orElse(0) + 1;

        List<GpxImportResponse> gpxImportResponses = new ArrayList<>();
        GpxImportResponse gpxImportResponse = new GpxImportResponse();
        for(MultipartFile file : files)
        {
            String filename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().toLowerCase() : "";

            if(segmentStatus.equals(GpxSegmentStatus.TRACKPOINT))
            {
                gpxImportResponse = filename.endsWith(".xlsx")
                        ? gpxParserService.parseTrackPointsFromExcel(file, route, nextOrder)
                        : gpxParserService.parseTrackPoints(file, route, nextOrder);
            }
            else if (segmentStatus.equals(GpxSegmentStatus.WAYPOINT)) {
                gpxImportResponse = filename.endsWith(".xlsx")
                        ? gpxParserService.parseWayPointsFromExcel(file, route, nextOrder)
                        : gpxParserService.parseWayPoints(file, route, nextOrder);
            }
            else {
                throw new RuntimeException("Unsupported segment status: " + segmentStatus);
            }


            nextOrder++;
           gpxImportResponses.add(gpxImportResponse);
        }
        if (segmentStatus.equals(GpxSegmentStatus.TRACKPOINT))
         gpxMergeService.mergeTrackPoints(route.getId());
        else if(segmentStatus.equals(GpxSegmentStatus.WAYPOINT))
            gpxMergeService.mergeWayPoints(route.getId());

        String message = segmentStatus.equals(GpxSegmentStatus.TRACKPOINT) ? "gpx segments uploaded for trackpoints " : "gpx segments uploaded for waypoints";
        return new ApiResponse<>(gpxImportResponses,message,201);

    }

    @Transactional(readOnly = true)
    public ApiResponse<List<GpxSegmentResponse>> getAllGpxSegment(@NonNull Integer routeId,GpxSegmentStatus segmentStatus)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );

        List<GpxSegment> gpxSegmentList = gpxSegmentRepo.findByRouteAndSegmentStatus(route,segmentStatus).orElseThrow(
                () -> new ResourceNotFoundException("gpx segments","route id",routeId) );

          if(gpxSegmentList.isEmpty()) throw new NoResourceFoundException("gpx segments");
        List<GpxSegmentResponse> gpxSegmentResponseList = gpxSegmentList.stream().map(gpxSegmentMapper::toGpxSegmentResponse).toList();
        String message = segmentStatus.equals(GpxSegmentStatus.TRACKPOINT) ? "gpx segment fetched for trackpoints" : "gpx segment fetched for waypoints";
        return new ApiResponse<>(gpxSegmentResponseList,message,200);

    }
    @Transactional(readOnly = true)
    public ApiResponse<GpxSegmentResponse> getGpxSegment(@NonNull Integer routeId , @NonNull Integer gpxSegmentId)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        GpxSegment gpxSegment = gpxSegmentRepo.findByIdAndRoute_Id(gpxSegmentId,route.getId()).orElseThrow(
                () -> new ResourceNotFoundException("gpx segment","id",gpxSegmentId)
        );
        GpxSegmentResponse segmentResponse = gpxSegmentMapper.toGpxSegmentResponse(gpxSegment);
        return new ApiResponse<>(segmentResponse, "gpx segment fetched",200);

    }

    @CacheEvict(value = "route-geoJson",key = "#routeId")
    @Transactional
    public ApiResponse<Void> reorderGpxSegment(@NonNull GpxSegmentOrderRequest segmentOrderRequest , @NonNull Integer routeId, @NonNull GpxSegmentStatus segmentStatus)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        List<GpxSegment> gpxSegments = gpxSegmentRepo.findByRouteAndSegmentStatus(route,segmentStatus).orElseThrow(
                () -> new ResourceNotFoundException("gpx segments","route id",routeId)
        );
        gpxSegments.forEach(gpxSegment -> gpxSegment.setOrderIndex(segmentOrderRequest.getSegmentIdWithOrder().get(gpxSegment.getId())));
        gpxSegmentRepo.saveAll(gpxSegments);
        if (segmentStatus.equals(GpxSegmentStatus.TRACKPOINT))
            gpxMergeService.mergeTrackPoints(route.getId());
        else if(segmentStatus.equals(GpxSegmentStatus.WAYPOINT))
            gpxMergeService.mergeWayPoints(route.getId());
        String message = segmentStatus.equals(GpxSegmentStatus.TRACKPOINT) ? "gpx segments reordered for trackpoints " : "gpx segments reordered for waypoints";
        return new ApiResponse<>(null,message,200);
    }

    @CacheEvict(value = "route-geoJson",key = "#routeId")
    @Transactional
    public ApiResponse<Void> deleteGpxSegment(@NonNull Integer gpxSegmentId,@NonNull Integer routeId,@NonNull GpxSegmentStatus segmentStatus)
    {

        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        GpxSegment gpxSegment = gpxSegmentRepo.findByIdAndRoute_Id(gpxSegmentId,route.getId()).orElseThrow(
                () -> new ResourceNotFoundException("gpx segment","id",gpxSegmentId)
        );
        try {
            if(segmentStatus.equals(GpxSegmentStatus.TRACKPOINT))
                trackPointRepo.deleteAllByGpxSegment_Id(gpxSegment.getId());
            else
            {
                trailSegmentRepo.deleteByGpxSegment_Id(gpxSegment.getId());
                poiRepo.deleteAllByWayPoint_GpxSegment_Id(gpxSegment.getId());
                accommodationRepo.deleteByWayPoint_GpxSegment_Id(gpxSegment.getId());
                wayPointRepo.deleteAllByGpxSegment_Id(gpxSegment.getId());
            }
            gpxSegmentRepo.deleteById(gpxSegment.getId());
            if (segmentStatus.equals(GpxSegmentStatus.TRACKPOINT))
                gpxMergeService.mergeTrackPoints(route.getId());
            else if(segmentStatus.equals(GpxSegmentStatus.WAYPOINT))
                gpxMergeService.mergeWayPoints(route.getId());
            String message = segmentStatus.equals(GpxSegmentStatus.TRACKPOINT) ? "gpx segment deleted for trackpoints " : "gpx segment deleted for waypoints";

            return new ApiResponse<>(null,message,200);
        }
        catch (Exception e)
        {
            throw new ResourceDeletionFailedException("gpx segment","id",gpxSegmentId);
        }
    }

    @CacheEvict(value = "route-geoJson",key = "#routeId")
    public ApiResponse<Void> remergeGpxSegment(Integer routeId,@NonNull GpxSegmentStatus segmentStatus)
    {

        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        if (segmentStatus.equals(GpxSegmentStatus.TRACKPOINT))
            gpxMergeService.mergeTrackPoints(route.getId());
        else if(segmentStatus.equals(GpxSegmentStatus.WAYPOINT))
            gpxMergeService.mergeWayPoints(route.getId());
        String message = segmentStatus.equals(GpxSegmentStatus.TRACKPOINT) ? "gpx segments merged for trackpoints " : "gpx segments merged for waypoints";

        return new ApiResponse<>(null,"gpx segments merged successfully",200);
    }
}
