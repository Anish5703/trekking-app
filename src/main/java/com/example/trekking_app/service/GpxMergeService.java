package com.example.trekking_app.service;

import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.exception.resource.ResourceMergeFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class GpxMergeService {

    private final RouteRepository routeRepo;
    private final GpxSegmentRepository gpxSegmentRepo;
    private final TrackPointRepository trackPointRepo;

    @Async("generalTaskExecutor")
    @Transactional
    public void mergeTrackPoints(@NonNull Integer routeId) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        List<GpxSegment> gpxSegments = gpxSegmentRepo.findByRoute(route).orElseThrow(
                        () -> new ResourceNotFoundException("gpx segments", "route id", routeId)
                ).stream()
                .sorted(Comparator.comparingInt(GpxSegment::getOrderIndex))
                .toList();

        try {
            AtomicInteger counter = new AtomicInteger(1);
            for (GpxSegment gpxSegment : gpxSegments) {
                List<TrackPoint>trackPoints = trackPointRepo.findByRoute_IdAndGpxSegment_IdOrderByLocalSequenceAsc(routeId,gpxSegment.getId());
                if(trackPoints.isEmpty()) continue;
                trackPoints.forEach(trackPoint -> trackPoint.setGlobalSequence(counter.getAndIncrement()));
                trackPointRepo.saveAll(trackPoints);

            }
            List<TrackPoint> activeTrackPoints = trackPointRepo.findByRouteAndIsDeletedFalseOrderByGlobalSequenceAsc(route).orElseThrow(
                    () -> new ResourceNotFoundException("active trackpoints","route id",routeId)
            );

            /** Fetch min elevation , max elevation , total distance in km */

            double minELe = activeTrackPoints.stream().filter(tp -> tp.getElevation() != null)
                    .mapToDouble(TrackPoint::getElevation).min().orElse(0.0);
            double maxEle = activeTrackPoints.stream().filter(tp -> tp.getElevation() != null)
                    .mapToDouble(TrackPoint::getElevation).max().orElse(0.0);
            double totalDist = calculateTotalDistance(activeTrackPoints);
            LineString path = generateRoutePath(activeTrackPoints,route);

            route.setMinElevation(minELe);
            route.setMaxElevation(maxEle);
            route.setDistanceInKm(Math.round(totalDist*100.0)/100.0);
            route.setPath(path);

            routeRepo.save(route);

        } catch (Exception e) {
            log.error("Failed to merge trackpoints with route {} : error -> {}",route.getName(),e.getLocalizedMessage());
            throw new ResourceMergeFailedException("trackpoints", "route id", routeId);
        }
    }
    public double calculateTotalDistance(List<TrackPoint> trackPoints)
    {
        double totalDist = 0;
        for(int i =1 ; i < trackPoints.size(); i++)
        {
            totalDist += haversineKm(trackPoints.get(i-1).getLatitude(),trackPoints.get(i-1).getLongitude(),
                    trackPoints.get(i).getLatitude() , trackPoints.get(i).getLongitude());
        }
        return totalDist;
    }

    public LineString generateRoutePath(List<TrackPoint> trackPoints,Route route)
    {
        if (trackPoints.size() < 2) {
            route.setPath(null);
            return null;
        }

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        Coordinate[] coordinates = trackPoints.stream()
                .map(tp -> new Coordinate(tp.getLongitude(), tp.getLatitude()))
                .toArray(Coordinate[]::new);

        LineString path = geometryFactory.createLineString(coordinates);
        return path;
    }
    public double haversineKm(double lat1 , double lon1 , double lat2 , double lon2)
    {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon /2) * Math.sin(dLon / 2);
        return R*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));

    }
}
