package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.destination.DestinationRequest;
import com.example.trekking_app.dto.destination.DestinationResponse;
import com.example.trekking_app.entity.Destination;

public class DestinationMapper {

    public Destination toEntity(DestinationRequest destinationRequest)
    {
        return Destination.builder()
                .name(destinationRequest.getName())
                .district(destinationRequest.getDistrict())
                .region(destinationRequest.getRegion())
                .build();
    }

    public DestinationResponse toResponse(Destination destination)
    {
        return DestinationResponse.builder()
                .id(destination.getId())
                .name(destination.getName())
                .district(destination.getDistrict())
                .region(destination.getRegion())
                .build();
    }
}
