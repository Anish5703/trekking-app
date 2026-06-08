package com.example.trekking_app.service.poi;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.poi.PoiRequest;
import com.example.trekking_app.dto.poi.PoiResponse;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.exception.resource.NoResourceFoundException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.resource.ResourceUpdateFailedException;
import com.example.trekking_app.mapper.PoiMapper;
import com.example.trekking_app.repository.POIRepository;
import com.example.trekking_app.repository.RouteRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;


@Slf4j
@Service
@RequiredArgsConstructor
public class PoiService {
    private final POIRepository poiRepo;
    private final RouteRepository routeRepo;
    private final PoiMapper poiMapper = new PoiMapper();

    @Transactional
    public ApiResponse<PoiResponse> createPoi(@NonNull Integer routeId, @NonNull PoiRequest poiRequest)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        POI poi = poiMapper.toEntity(poiRequest,route);
        POI newPoi = poiRepo.save(poi);
        PoiResponse poiResponse = poiMapper.toPoiResponse(newPoi);
        return new ApiResponse<>(poiResponse,"new poi created",201);
    }
    @Transactional
    public ApiResponse<PoiResponse> updatePoi(@NonNull Integer routeId,@NonNull Integer poiId,@NonNull PoiRequest poiRequest)

    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        POI poi = poiRepo.findByIdAndRouteId(routeId,poiId).orElseThrow(
                () -> new ResourceNotFoundException("poi","route id and poi id",String.format("%d and %d respectively",routeId,poiId))
        );
        POI updatePoi = poiMapper.toEntity(poiRequest,route);
        updatePoi.setId(poi.getId());
        updatePoi.setWayPoint(poi.getWayPoint());
        try{
            POI updatedPoi = poiRepo.save(updatePoi);
            PoiResponse poiResponse = poiMapper.toPoiResponse(updatedPoi);
            return new ApiResponse<>(poiResponse,"poi updated",200);
        }
        catch (Exception e)
        {
            log.error("failed to update poi : {} : {}",e.getClass(),e.getLocalizedMessage());
            throw new ResourceUpdateFailedException("poi","id",poiId);
        }

    }

    @Transactional
    public ApiResponse<Void> deletePoi(@NonNull Integer routeId,@NonNull Integer poiId)
    {
        POI poi = poiRepo.findByIdAndRouteId(routeId,poiId).orElseThrow(
                () -> new ResourceNotFoundException("poi","route id and poi id",String.format("%d and %d respectively",routeId,poiId))
        );
        poiRepo.delete(poi);
        return new ApiResponse<>(null,"poi deleted",200);
    }

    public ApiResponse<Page<PoiResponse>> getAllPoi(@NonNull Integer routeId,Integer page , Integer size) {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        Pageable  pageable = PageRequest.of(page,size);
        Page<PoiResponse> poiResponses = poiRepo.findByRoute_id(route.getId(),pageable).map(poiMapper::toPoiResponse);
        if(poiResponses.isEmpty()) throw new NoResourceFoundException("poi");
        return new ApiResponse<>(poiResponses,"pois fetched",200);

    }

    public ApiResponse<PoiResponse> getPoi(@NonNull Integer routeId,@NonNull Integer poiId)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        POI poi = poiRepo.findByIdAndRouteId(poiId,route.getId()).orElseThrow(
                () -> new ResourceNotFoundException("poi","id",poiId)
        );
        PoiResponse poiResponse = poiMapper.toPoiResponse(poi);
        return new ApiResponse<>(poiResponse,"poi fetched",200);
    }
}
