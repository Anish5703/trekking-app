package com.example.trekking_app.service.xlsx;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.poi.XlsxImportResponse;
import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrailSegment;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import com.example.trekking_app.repository.AccommodationRepository;
import com.example.trekking_app.repository.POIRepository;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrailSegmentRepository;
import com.example.trekking_app.service.gpx.GpxMergeService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class XlsxIngestionService {

    private final XlsxParser parser;
    private final RouteRepository routeRepo;
    private final ComputeService computeService;


    public ApiResponse<XlsxImportResponse> uploadXlsx(@NonNull Integer routeId, @NonNull MultipartFile file) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        ParseOutput parserOutput = parser.parse(file, route);
        CompletableFuture<Integer> poiCount = computeService.saveAllPOI(parserOutput.pois());
        CompletableFuture<Integer> accommodationCount = computeService.saveAllAccommodation(parserOutput.accommodations());
        CompletableFuture<Integer> trailSegmentCount = computeService.saveAllTrailSegment(parserOutput.trailSegments());
        CompletableFuture.allOf(poiCount,accommodationCount,trailSegmentCount);
        XlsxImportResponse importResponse = XlsxImportResponse.builder()
                .numberOfRows(parserOutput.rawRows().size())
                .numberOfPOI(poiCount.join())
                .numberOfAccommodation(accommodationCount.join())
                .numberOfTrailSegment(trailSegmentCount.join())
                .numberOfWayPoint(parserOutput.wayPoints().size())
                .build();
        return new ApiResponse<>(importResponse, "xlsx file parsed successfully", 200);

    }

}
