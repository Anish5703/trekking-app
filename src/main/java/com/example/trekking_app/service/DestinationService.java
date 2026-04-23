package com.example.trekking_app.service;

import com.example.trekking_app.dto.destination.DestinationRequest;
import com.example.trekking_app.dto.destination.DestinationResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.entity.Destination;
import com.example.trekking_app.exception.resource.*;
import com.example.trekking_app.mapper.DestinationMapper;
import com.example.trekking_app.repository.DestinationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DestinationService {

    private final DestinationRepository destinationRepo;
    private final DestinationMapper destinationMapper;


    public DestinationService(DestinationRepository destinationRepo) {
        this.destinationRepo = destinationRepo;
        this.destinationMapper = new DestinationMapper();
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<DestinationResponse>> getAllDestination()
    {
        List<Destination> destinationList = destinationRepo.findAll();
        if(destinationList.isEmpty())
            throw new NoResourceFoundException("destination");
        List<DestinationResponse> destinationResponseList = new ArrayList<>();
        destinationList.forEach(destination -> destinationResponseList.add(destinationMapper.toResponse(destination)));
        return new ApiResponse<>(destinationResponseList,"all destination fetched",200);
    }

    @Transactional
    public ApiResponse<DestinationResponse> createDestination(DestinationRequest destinationRequest) {

            boolean isDuplicate = destinationRepo.existsByNameAndDistrictAndRegion(destinationRequest.getName(),
                    destinationRequest.getDistrict(), destinationRequest.getRegion());
            if (isDuplicate) throw new DuplicateResourceFoundException("destination","name, district, region",
                    String.format("%s , %s , %s",destinationRequest.getName(),destinationRequest.getDistrict() , destinationRequest.getRegion()));

            try{
            Destination destination = destinationRepo.save(destinationMapper.toEntity(destinationRequest));
            DestinationResponse destinationResponse = destinationMapper.toResponse(destination);
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
    public ApiResponse<Integer> deleteDestination(int destinationId)
    {

            boolean isExists = destinationRepo.existsById(destinationId);
            if(!isExists) throw new ResourceNotFoundException("destination","id",destinationId);
            try{
                destinationRepo.deleteById(destinationId);
            return new ApiResponse<>(destinationId,"destination deleted",200);
        }
        catch (Exception e)
        {
            log.error("Failed to delete destination "+ destinationId);
            throw new ResourceDeletionFailedException("destination" ,"id" ,destinationId);
        }
    }
}
