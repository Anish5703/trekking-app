package com.example.trekking_app.service;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.trackpoint.TrackPointDetails;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.mapper.TrackPointMapper;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TrackPointService {

    private final TrackPointRepository trackPointRepo;
    private final RouteRepository routeRepo;
    private final TrackPointMapper trackPointMapper;

    public TrackPointService(TrackPointRepository trackPointRepo, RouteRepository routeRepo)
    {
        this.trackPointRepo = trackPointRepo;
        this.routeRepo = routeRepo;
        this.trackPointMapper = new TrackPointMapper();
    }

    @Transactional
    public ApiResponse<List<TrackPointDetails>> getTrackPoints(int routeId)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );

        List<TrackPoint> trackPointList = route.getTrackPoints();
        List<TrackPointDetails> trackPointDetailsList = new ArrayList<>();
        trackPointList.forEach(trackPoint -> trackPointDetailsList.add(trackPointMapper.toTrackPointDetails(trackPoint)));
        return  new ApiResponse<>(trackPointDetailsList,"trackpoints fetched",200);
    }
}
