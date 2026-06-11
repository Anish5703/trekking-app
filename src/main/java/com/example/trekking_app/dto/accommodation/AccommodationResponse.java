package com.example.trekking_app.dto.accommodation;

import com.example.trekking_app.model.ElectricitySource;
import com.example.trekking_app.model.POIType;
import com.example.trekking_app.model.WaterSource;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AccommodationResponse
{
    private Integer id;
    private Integer routeId;
    private String name;
    private Double latitude;
    private Double longitude;
    private Double elevation;
    private POIType poiType;
    private String description;
    private String address;
    private String contactNumber;
    private List<String> imageUrls;
    private Integer totalRooms;
    private Double priceNepali;
    private Double priceForeigner;
    private ElectricitySource electricitySource;
    private WaterSource waterSource;
    private Boolean hasFirstAid;










}
