package com.example.trekking_app.service;

import com.example.trekking_app.dto.route.GpxImportResponse;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.exception.route.RouteNotFoundException;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class GpxParserService {

    private final RouteRepository routeRepo;
    private final TrackPointRepository trackPointRepo;

    public GpxParserService(RouteRepository routeRepo, TrackPointRepository trackPointRepo)
    {
      this.routeRepo = routeRepo;
      this.trackPointRepo = trackPointRepo;
    }

    public GpxImportResponse importGpx(MultipartFile file, int routeId)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new RouteNotFoundException("No route found with id "+routeId)
        );

        return null;
    }
}
