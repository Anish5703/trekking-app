package com.example.trekking_app.service.trackpoints;

import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.model.RouteStatus;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GpxMergeHelper {

    private final TrackPointRepository trackPointRepo;
    private final RouteRepository routeRepo;

    @Transactional                          // short transaction — one UPDATE, releases immediately
    public void assignGlobalSequences(Integer routeId) {
        trackPointRepo.updateGlobalSequences(routeId);
    }

    @Transactional                          // short transaction — load, compute, save, release
    public void finalizeRoute(Integer routeId) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );

        List<TrackPoint> activeTrackPoints = trackPointRepo
                .findByRouteAndIsDeletedFalseOrderByGlobalSequenceAsc(route)
                .orElseThrow(() -> new ResourceNotFoundException("active trackpoints", "route id", routeId));

        double minEle = trackPointRepo.findMinElevation(routeId).orElse(0.0);
        double maxEle = trackPointRepo.findMaxElevation(routeId).orElse(0.0);
        double totalDist = calculateTotalDistance(activeTrackPoints);
        LineString path = generateRoutePath(activeTrackPoints, route);

        route.setMinElevation(minEle);
        route.setMaxElevation(maxEle);
        route.setDistanceInKm(Math.round(totalDist * 100.0) / 100.0);
        route.setPath(path);
        route.setRouteStatus(RouteStatus.MERGED);
        routeRepo.save(route);
    }

    @Transactional
    public void updateRouteStatus(Integer routeId, RouteStatus status) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        route.setRouteStatus(status);
        routeRepo.save(route);
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
