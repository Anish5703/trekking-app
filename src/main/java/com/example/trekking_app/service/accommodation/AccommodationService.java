package com.example.trekking_app.service.accommodation;

import com.example.trekking_app.dto.accommodation.AccommodationRequest;
import com.example.trekking_app.dto.accommodation.AccommodationResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.Image;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.exception.resource.DuplicateResourceFoundException;
import com.example.trekking_app.exception.resource.ResourceCreationFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.mapper.AccommodationMapper;
import com.example.trekking_app.model.EntityType;
import com.example.trekking_app.repository.AccommodationRepository;
import com.example.trekking_app.repository.ImageRepository;
import com.example.trekking_app.repository.RouteRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccommodationService
{
    private final AccommodationRepository accommodationRepo;
    private final RouteRepository routeRepo;
    private final ImageRepository imageRepo;
    private final AccommodationMapper accommodationMapper = new AccommodationMapper();


    @Transactional
    public ApiResponse<AccommodationResponse> createAccommodation(@NonNull Integer routeId,@NonNull AccommodationRequest req)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        boolean existsByRouteAndLatitudeAnLongitude = accommodationRepo.existsByRoute_IdAndLongitudeAndLatitude(route,req.getLongitude(),req.getLatitude());
        if(existsByRouteAndLatitudeAnLongitude) throw new DuplicateResourceFoundException("accommodation","route id , longitude and latitude",String.format("%d , %f and %f",route.getId(),req.getLongitude(),req.getLatitude()));
        Accommodation acc = accommodationMapper.toEntity(req,route);
        try
        {
            Accommodation newAcc = accommodationRepo.save(acc);
            AccommodationResponse accResponse = accommodationMapper.toAccommodationResponse(acc,null);
            return new ApiResponse<>(accResponse,"accommodation created",201);
        }
        catch (Exception e)
        {
            throw new ResourceCreationFailedException("accommodation","name",req.getName());
        }
    }
    @Transactional(readOnly = true)
    public ApiResponse<AccommodationResponse> getAccommodation(@NonNull Integer routeId,@NonNull Integer accommodationId)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        Accommodation acc = accommodationRepo.findById(accommodationId).orElseThrow(
                () -> new ResourceNotFoundException("accommodation","id",accommodationId)
        );
        List<String> imageUrls = imageRepo.findByEntityTypeAndEntityId(EntityType.ACCOMMODATION,acc.getId()).stream()
                .map(Image::getUrl).toList();
        AccommodationResponse accResponse = accommodationMapper.toAccommodationResponse(acc,imageUrls);
        return new ApiResponse<>(accResponse,"accommodation fetched",200);
    }
}
