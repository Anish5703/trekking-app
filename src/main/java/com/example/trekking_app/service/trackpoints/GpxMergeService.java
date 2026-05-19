package com.example.trekking_app.service.trackpoints;

import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.exception.resource.ResourceMergeFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.model.RouteStatus;
import com.example.trekking_app.repository.GpxSegmentRepository;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class GpxMergeService {

   private final GpxMergeHelper mergeHelper;
   public void mergeTrackPoints(@NonNull Integer routeId){
    mergeHelper.updateRouteStatus(routeId, RouteStatus.MERGING);
    try {
        mergeHelper.assignGlobalSequences(routeId);
        mergeHelper.finalizeRoute(routeId);
    } catch (Exception e) {
        log.error("Failed to merge trackpoints for route id {}: {}", routeId, e.getLocalizedMessage());
        mergeHelper.updateRouteStatus(routeId, RouteStatus.FAILED);
        throw new ResourceMergeFailedException("trackpoints", "route id", routeId);
    }
}


}
