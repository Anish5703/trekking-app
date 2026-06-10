package com.example.trekking_app.service.route;

import com.example.trekking_app.dto.destination.DestinationRequest;
import com.example.trekking_app.dto.destination.DestinationResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.entity.Destination;
import com.example.trekking_app.entity.Image;
import com.example.trekking_app.exception.resource.*;
import com.example.trekking_app.mapper.DestinationMapper;
import com.example.trekking_app.model.EntityType;
import com.example.trekking_app.repository.DestinationRepository;
import com.example.trekking_app.repository.ImageRepository;
import com.example.trekking_app.service.image.CloudinaryService;
import com.example.trekking_app.service.image.ImageService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DestinationService {

    private final DestinationRepository destinationRepo;
    private final DestinationMapper destinationMapper = new DestinationMapper();
   private final ImageRepository imageRepo;
   private final ImageService imageService;


    @Transactional(readOnly = true)
    public ApiResponse<List<DestinationResponse>> getAllDestination()
    {
        List<Destination> destinationList = destinationRepo.findAll();
        if(destinationList.isEmpty())
            throw new NoResourceFoundException("destination");
        List<DestinationResponse> destinationResponseList = new ArrayList<>();
        destinationList.forEach(destination -> {
                List<String> imageUrls = imageRepo.findByEntityTypeAndEntityId(EntityType.DESTINATION,destination.getId()).stream().map(Image::getUrl).toList();
                destinationResponseList.add(destinationMapper.toResponse(destination,imageUrls));}
        );
        return new ApiResponse<>(destinationResponseList,"all destination fetched",200);
    }

    @Transactional
    public ApiResponse<DestinationResponse> createDestination(DestinationRequest destinationRequest)
    {

            boolean isDuplicate = destinationRepo.existsByNameAndDistrictAndRegion(destinationRequest.getName(),
                    destinationRequest.getDistrict(), destinationRequest.getRegion());

            if (isDuplicate) throw new DuplicateResourceFoundException("destination","name, district, region",
                    String.format("%s , %s , %s",destinationRequest.getName(),destinationRequest.getDistrict() , destinationRequest.getRegion()));

            try {
            Destination destination = destinationRepo.save(destinationMapper.toEntity(destinationRequest));
                List<String> imagesUrl = new ArrayList<>();
            DestinationResponse destinationResponse = destinationMapper.toResponse(destination,imagesUrl);
            return new ApiResponse<>(destinationResponse, "New destination created", 201);

        } catch (Exception ex) {
            log.error("Failed to create destination {}", ex.getLocalizedMessage());
            throw new ResourceCreationFailedException("destination","name",destinationRequest.getName());
        }
    }

    @Transactional
    public ApiResponse<DestinationResponse> updateDestination(DestinationRequest destinationRequest, int destinationId)
    {
            Destination destination = destinationRepo.findById(destinationId).orElseThrow(
                    () -> new ResourceNotFoundException("destination","id",destinationId)
            );
            try{
            destination.setName(destinationRequest.getName());
            destination.setDistrict(destinationRequest.getDistrict());
            destination.setRegion(destinationRequest.getRegion());

            Destination modifiedDestination = destinationRepo.save(destination);
            return new ApiResponse<>(destinationMapper.toResponse(modifiedDestination),
                    "destination updated", 200);

        } catch (Exception ex) {
            log.error("Failed to update destination " + destinationId);
            throw new ResourceUpdateFailedException("destination","name",destinationRequest.getName());
        }
    }

    @Transactional
    public ApiResponse<Void> deleteDestination(int destinationId)
    {

            Destination destination = destinationRepo.findById(destinationId).orElseThrow(
                    () -> new ResourceNotFoundException("destination","id",destinationId));
            try{
                if(!destination.getRoutes().isEmpty())
                    throw new ResourceDeletionFailedException("failed to delete destination ! delete routes associated with destination first");
               //search for associated images
                List<Image> images = imageRepo.findByEntityTypeAndEntityId(EntityType.DESTINATION,destination.getId());
                destinationRepo.deleteById(destination.getId());
                if(!images.isEmpty())images.forEach(this::deleteImagesForDestination);
            return new ApiResponse<>(null,"destination deleted",200);
        }
        catch (Exception e)
        {
            log.error("Failed to delete destination "+ destinationId);
            throw new ResourceDeletionFailedException("destination" ,"id" ,destinationId);
        }
    }

    public void deleteImagesForDestination(Image image)
    {
        try{
            imageService.deleteImage(image.getId());
        }
        catch (Exception ignored) {
            log.error("failed to delete image id"+image.getId());
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse<DestinationResponse> getDestination(@NonNull Integer destinationId)
    {
        Destination destination = destinationRepo.findById(destinationId).orElseThrow(
                () -> new ResourceNotFoundException("destination","id",destinationId)
        );
        List<String> imagesUrl = imageRepo.findByEntityTypeAndEntityId(EntityType.DESTINATION,destination.getId()).stream().map(Image::getUrl).toList();
        DestinationResponse dstResponse = destinationMapper.toResponse(destination,imagesUrl);
        return new ApiResponse<>(dstResponse,"destination fetched",200);
    }
}
