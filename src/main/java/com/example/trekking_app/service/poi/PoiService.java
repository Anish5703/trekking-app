package com.example.trekking_app.service.poi;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.poi.NearbyPoiProjection;
import com.example.trekking_app.dto.poi.NearbyPoiResponse;
import com.example.trekking_app.dto.poi.PoiRequest;
import com.example.trekking_app.dto.poi.PoiResponse;
import com.example.trekking_app.dto.route.NearbyRequest;
import com.example.trekking_app.entity.Image;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.exception.resource.NoResourceFoundException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.resource.ResourceUpdateFailedException;
import com.example.trekking_app.mapper.PoiMapper;
import com.example.trekking_app.model.EntityType;
import com.example.trekking_app.repository.ImageRepository;
import com.example.trekking_app.repository.POIRepository;
import com.example.trekking_app.repository.RouteRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoiService {
    private final POIRepository poiRepo;
    private final RouteRepository routeRepo;
    private final ImageRepository imageRepo;
    private final PoiMapper poiMapper = new PoiMapper();

    @CacheEvict(value = "route-geoJson" ,key="#routeId" , allEntries = true)
    @Transactional
    public ApiResponse<PoiResponse> createPoi(@NonNull Integer routeId, @NonNull PoiRequest poiRequest)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        POI poi = poiMapper.toEntity(poiRequest,route);
        POI newPoi = poiRepo.save(poi);
        PoiResponse poiResponse = poiMapper.toPoiResponse(newPoi,null);
        return new ApiResponse<>(poiResponse,"new poi created",201);
    }
    @CacheEvict(value = "route-geoJson" ,key="#routeId" , allEntries = true)
    @Transactional
    public ApiResponse<PoiResponse> updatePoi(@NonNull Integer routeId,@NonNull Integer poiId,@NonNull PoiRequest poiRequest)

    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        POI poi = poiRepo.findByIdAndRouteId(poiId,routeId).orElseThrow(
                () -> new ResourceNotFoundException("poi","route id and poi id",String.format("%d and %d respectively",routeId,poiId))
        );
        POI updatePoi = poiMapper.toEntity(poiRequest,route);
        updatePoi.setId(poi.getId());
        updatePoi.setWayPoint(poi.getWayPoint());
        try{
            POI updatedPoi = poiRepo.save(updatePoi);
            List<String> imageUrls = imageRepo.findByEntityTypeAndEntityId(EntityType.POI,poi.getId()).stream().map(Image::getUrl).toList();
            PoiResponse poiResponse = poiMapper.toPoiResponse(updatedPoi,imageUrls);
            return new ApiResponse<>(poiResponse,"poi updated",200);
        }
        catch (Exception e)
        {
            log.error("failed to update poi : {} : {}",e.getClass(),e.getLocalizedMessage());
            throw new ResourceUpdateFailedException("poi","id",poiId);
        }

    }
    @CacheEvict(value = "route-geoJson" ,key="#routeId" , allEntries = true)
    @Transactional
    public ApiResponse<Void> deletePoi(@NonNull Integer routeId,@NonNull Integer poiId)
    {
        POI poi = poiRepo.findByIdAndRouteId(poiId,routeId).orElseThrow(
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
        Page<PoiResponse> poiResponses = poiRepo.findByRoute_id(route.getId(),pageable).map(poi ->
        {
            List<String> imageUrls = imageRepo.findByEntityTypeAndEntityId(EntityType.POI,poi.getId()).stream().map(Image::getUrl).toList();
            return poiMapper.toPoiResponse(poi,imageUrls);

        });
        if(poiResponses.isEmpty()) throw new NoResourceFoundException("poi");
        return new ApiResponse<>(poiResponses,"pois fetched",200);

    }

    @Transactional(readOnly = true)
    public ApiResponse<PoiResponse> getPoi(@NonNull Integer routeId,@NonNull Integer poiId)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route", "id", routeId)
        );
        POI poi = poiRepo.findByIdAndRouteId(poiId,route.getId()).orElseThrow(
                () -> new ResourceNotFoundException("poi","id",poiId)
        );
        List<String> imageUrls = imageRepo.findByEntityTypeAndEntityId(EntityType.POI,poi.getId()).stream().map(Image::getUrl).toList();
        PoiResponse poiResponse = poiMapper.toPoiResponse(poi,imageUrls);
        return new ApiResponse<>(poiResponse,"poi fetched",200);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<NearbyPoiResponse>> getPoiNearby(NearbyRequest nearbyRequest)
    {
         List<NearbyPoiProjection> nearbyPois = poiRepo.findNearbyPois(nearbyRequest.getLongitude(), nearbyRequest.getLatitude(),
                                                                 nearbyRequest.getRadiusMeters(), nearbyRequest.getLimit());
        if(nearbyPois.isEmpty()) throw new NoResourceFoundException("nearby poi");
        List<NearbyPoiResponse> nearbyPoiResponses = nearbyPois.stream().map(nearbyPoi ->
        {
            List<String> imageUrls = imageRepo.findByEntityTypeAndEntityId(EntityType.POI,nearbyPoi.getId()).stream().map(Image::getUrl).toList();
             return poiMapper.toNearbyPoiResponse(nearbyPoi,imageUrls);
        }).toList();
        if(nearbyPoiResponses.isEmpty()) throw new NoResourceFoundException("nearby pois");
        return new ApiResponse<>(nearbyPoiResponses,"nearby poi fetched",200);

    }
}
