package com.example.trekking_app.service.accommodation;

import com.example.trekking_app.dto.accommodation.AccommodationRequest;
import com.example.trekking_app.dto.accommodation.AccommodationResponse;
import com.example.trekking_app.dto.accommodation.NearbyAccommodationProjection;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.NearbyRequest;
import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.Image;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.exception.resource.*;
import com.example.trekking_app.mapper.AccommodationMapper;
import com.example.trekking_app.model.EntityType;
import com.example.trekking_app.repository.AccommodationRepository;
import com.example.trekking_app.repository.ImageRepository;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.service.image.ImageService;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccommodationService
{
    private final AccommodationRepository accommodationRepo;
    private final RouteRepository routeRepo;
    private final ImageRepository imageRepo;
    private final ImageService imageService;
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
        catch (DataAccessException e)
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
    @Transactional(readOnly = true)
    public ApiResponse<Page<AccommodationResponse>> getAllAccommodation(@NonNull Integer routeId,Integer page , Integer size)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        Pageable pageable = PageRequest.of(page,size);
        Page<Accommodation> accommodations = accommodationRepo.findByRoute_Id(route.getId(),pageable);
        if(accommodations.isEmpty()) throw new NoResourceFoundException("no accommodation found with route id "+route.getId());
        Page<AccommodationResponse>  accommodationResponses = accommodations.map(acc -> {
           List<String> imageUrls = imageRepo.findByEntityTypeAndEntityId(EntityType.ACCOMMODATION,acc.getId()).stream().map(Image::getUrl).toList();
           return accommodationMapper.toAccommodationResponse(acc,imageUrls);
       });
        return new ApiResponse<>(accommodationResponses,"accommodations fetched",200);

    }


@Transactional
    public ApiResponse<AccommodationResponse> updateAccommodation(@NonNull Integer routeId, @NonNull Integer accommodationId, @Valid AccommodationRequest accommodationRequest)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        Accommodation acc = accommodationRepo.findById(accommodationId).orElseThrow(
                () -> new ResourceNotFoundException("accommodation","id",accommodationId)
        );
        Accommodation newAcc = accommodationMapper.toEntity(accommodationRequest,acc.getRoute());
        newAcc.setId(acc.getId());
        try {
            accommodationRepo.save(newAcc);
            List<String> imageUrls = imageRepo.findByEntityTypeAndEntityId(EntityType.ACCOMMODATION, acc.getId()).stream()
                    .map(Image::getUrl).toList();
            AccommodationResponse accResponse = accommodationMapper.toAccommodationResponse(newAcc, imageUrls);
            return new ApiResponse<>(accResponse, "accommodation updated", 200);
        } catch (DataAccessException e) {
            log.error("exception thrown while updating accommodation {} , {}",e.getClass(),e.getMessage());
            throw new ResourceUpdateFailedException("accommodation","id",accommodationId);
        }
    }

    @Transactional
    public ApiResponse<Void> deleteAccommodation(@NonNull Integer routeId , @NonNull Integer accommodationId)
    {
        Route route = routeRepo.findById(routeId).orElseThrow(
                () -> new ResourceNotFoundException("route","id",routeId)
        );
        Accommodation acc = accommodationRepo.findById(accommodationId).orElseThrow(
                () -> new ResourceNotFoundException("accommodation","id",accommodationId)
        );
        try
        {
            List<Image> images = imageRepo.findByEntityTypeAndEntityId(EntityType.ACCOMMODATION,acc.getId());
           accommodationRepo.delete(acc);
           images.forEach(this::deleteImagesForAccommodation);
           return new ApiResponse<>(null,"accommodation deleted",200);
        }
        catch (Exception e)
        {
            log.error("exception thrown while deleting accommodation {} , {}",e.getClass(),e.getLocalizedMessage());
            throw new ResourceDeletionFailedException("accommodation","id",accommodationId);
        }
    }


    // helper method for image deleting
    public void deleteImagesForAccommodation(Image image)
    {
        try{
            imageService.deleteImage(image.getId());
        }
        catch (Exception ignored) {
            log.error("failed to delete image id"+image.getId());
        }
    }

    public ApiResponse<List<AccommodationResponse>> getAccommodationNearby(@NonNull @Valid NearbyRequest request)
    {
        List<NearbyAccommodationProjection> nearbyAccommodations = accommodationRepo.findNearbyAccommodation(request.getLongitude(), request.getLatitude(),request.getRadiusMeters(),request.getLimit());
        if(nearbyAccommodations.isEmpty()) throw new NoResourceFoundException("no accommodation found nearby");
        List<AccommodationResponse> accommodationResponses = nearbyAccommodations.stream().map( nearby ->
                {
                    Accommodation acc = accommodationRepo.findById(nearby.getId()).orElse(null);
                    if(acc==null) return null;
                    List<String> imageUrls = imageRepo.findByEntityTypeAndEntityId(EntityType.ACCOMMODATION,acc.getId()).stream().map(Image::getUrl).toList();
                    return accommodationMapper.toAccommodationResponse(acc,imageUrls);
                }
        ).filter(Objects::nonNull).toList();
        return new ApiResponse<>(accommodationResponses,"nearby accommodation fetched",200);
    }
}
