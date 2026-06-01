package com.example.trekking_app.service.xlsx;

import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.TrailSegment;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import com.example.trekking_app.repository.AccommodationRepository;
import com.example.trekking_app.repository.POIRepository;
import com.example.trekking_app.repository.TrailSegmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComputeService {
    private final POIRepository poiRepo;
    private final AccommodationRepository accommodationRepo;
    private final TrailSegmentRepository trailSegmentRepo;

    @Async("generalTaskExecutor")
    @Transactional
    public CompletableFuture<Integer> saveAllPOI(List<POI> pois)
    {
        try{
            if(pois.isEmpty()) return CompletableFuture.completedFuture(0);
            Integer size = poiRepo.saveAll(pois).size();
            return CompletableFuture.completedFuture(size);
        }
        catch (Exception e)
        {
            log.error("exception thrown while saving pois : {} : {}",e.getClass(),e.getLocalizedMessage());
            throw new FileParsingFailedException("failed to save pois");
        }
    }
    @Transactional
    @Async(value = "generalTaskExecutor")
    public CompletableFuture<Integer> saveAllAccommodation(List<Accommodation> acc)
    {
        try{
            if(acc.isEmpty()) return CompletableFuture.completedFuture(0);
            Integer size = accommodationRepo.saveAll(acc).size();
            return CompletableFuture.completedFuture(size);
        }
        catch (Exception e)
        {
            log.error("exception thrown while saving accommodation : {} : {}",e.getClass(),e.getLocalizedMessage());
            throw new FileParsingFailedException("failed to save accommodation");
        }
    }

    @Transactional
    @Async(value = "generalTaskExecutor")
    public CompletableFuture<Integer> saveAllTrailSegment(List<TrailSegment> trailSegments)
    {
        try{
            if(trailSegments.isEmpty()) return CompletableFuture.completedFuture(0);
            Integer size = trailSegmentRepo.saveAll(trailSegments).size();
            return CompletableFuture.completedFuture(size);
        }
        catch (Exception e)
        {
            log.error("exception thrown while saving trail segments : {} : {}",e.getClass(),e.getLocalizedMessage());
            throw new FileParsingFailedException("failed to save segments");
        }
    }
}
