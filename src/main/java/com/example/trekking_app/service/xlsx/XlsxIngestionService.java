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

@Service
@RequiredArgsConstructor
@Slf4j
public class XlsxIngestionService {

    private final XlsxParser parser;
    private final RouteRepository routeRepo;
    private final POIRepository poiRepo;
    private final AccommodationRepository accommodationRepo;
    private final TrailSegmentRepository trailSegmentRepo;
    private final GpxMergeService gpxMergeService;


    public ApiResponse<XlsxImportResponse> uploadXlsx(@NonNull Integer routeId, @NonNull MultipartFile file) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        ParseOutput parserOutput = parser.parse(file, route);
        Integer poiCount = saveAllPOI(parserOutput.pois());
        Integer accommodationCount = saveAllAccommodation(parserOutput.accommodations());
        Integer trailSegmentCount = saveAllTrailSegment(parserOutput.trailSegments());
        XlsxImportResponse importResponse = XlsxImportResponse.builder()
                .numberOfRows(parserOutput.rawRows().size())
                .numberOfPOI(poiCount)
                .numberOfAccommodation(accommodationCount)
                .numberOfTrailSegment(trailSegmentCount)
                .numberOfWayPoint(parserOutput.wayPoints().size())
                .build();
        return new ApiResponse<>(importResponse, "xlsx file parsed successfully", 200);

    }

    @Transactional
    @Async(value = "generalTaskExecutor")
    public Integer saveAllPOI(List<POI> pois)
    {
        try{
            if(pois.isEmpty()) return 0;
            return poiRepo.saveAll(pois).size();
        }
        catch (Exception e)
        {
            log.error("exception thrown while saving pois : {} : {}",e.getClass(),e.getLocalizedMessage());
            throw new FileParsingFailedException("failed to save pois");
        }
    }

    @Transactional
    @Async(value = "generalTaskExecutor")
    public Integer saveAllAccommodation(List<Accommodation> acc)
    {
        try{
            if(acc.isEmpty()) return 0;
            return accommodationRepo.saveAll(acc).size();
        }
        catch (Exception e)
        {
            log.error("exception thrown while saving accommodation : {} : {}",e.getClass(),e.getLocalizedMessage());
            throw new FileParsingFailedException("failed to save accommodation");
        }
    }

    @Transactional
    @Async(value = "generalTaskExecutor")
    public Integer saveAllTrailSegment(List<TrailSegment> trailSegments)
    {
        try{
            if(trailSegments.isEmpty()) return 0;
            return trailSegmentRepo.saveAll(trailSegments).size();
        }
        catch (Exception e)
        {
            log.error("exception thrown while saving trail segments : {} : {}",e.getClass(),e.getLocalizedMessage());
            throw new FileParsingFailedException("failed to save segments");
        }
    }
}
