package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.accommodation.AccommodationRequest;
import com.example.trekking_app.dto.accommodation.AccommodationResponse;
import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.Route;

import java.util.List;

public class AccommodationMapper {



    public Accommodation toEntity(AccommodationRequest req, Route route)
    {
        Accommodation accommodation = new Accommodation();
        // POI fields
        accommodation.setName(req.getName());
        accommodation.setLatitude(req.getLatitude());
        accommodation.setLongitude(req.getLongitude());
        accommodation.setElevation(req.getElevation());
        accommodation.setType(req.getPoiType());
        accommodation.setDescription(req.getDescription());
        accommodation.setRoute(route);
        // Accommodation fields
        accommodation.setAddress(req.getAddress());
        accommodation.setContactNumber(req.getContactNumber());
        accommodation.setTotalRooms(req.getTotalRooms());
        accommodation.setPriceNepali(req.getPriceNepali());
        accommodation.setPriceForeigner(req.getPriceForeigner());
        accommodation.setElectricitySource(req.getElectricitySource());
        accommodation.setWaterSource(req.getWaterSource());
        accommodation.setHasFirstAid(req.getHasFirstAid());
        return accommodation;
    }

    public AccommodationResponse toAccommodationResponse(Accommodation acc, List<String> imageUrls) {
        return AccommodationResponse.builder()
                .id(acc.getId())
                .routeId(acc.getRoute() != null ? acc.getRoute().getId() : null)
                .name(acc.getName())
                .latitude(acc.getLatitude())
                .longitude(acc.getLongitude())
                .elevation(acc.getElevation())
                .poiType(acc.getType())
                .description(acc.getDescription())
                .address(acc.getAddress())
                .contactNumber(acc.getContactNumber())
                .imageUrls(imageUrls)
                .totalRooms(acc.getTotalRooms())
                .priceNepali(acc.getPriceNepali())
                .priceForeigner(acc.getPriceForeigner())
                .electricitySource(acc.getElectricitySource())
                .waterSource(acc.getWaterSource())
                .hasFirstAid(acc.getHasFirstAid())
                .build();
    }

}
