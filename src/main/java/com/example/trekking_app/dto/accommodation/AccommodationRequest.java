package com.example.trekking_app.dto.accommodation;

import com.example.trekking_app.model.ElectricitySource;
import com.example.trekking_app.model.POIType;
import com.example.trekking_app.model.WaterSource;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AccommodationRequest
{
    @NonNull
    private String name;
    @NonNull
    private Double latitude;
    @NonNull
    private Double longitude;
    private Double elevation;
    @NonNull
    private POIType poiType;
    private String description;
    private String address;
    private String contactNumber;
    private Integer totalRooms;
    private Double priceNepali;
    private Double priceForeigner;
    private ElectricitySource electricitySource;
    private WaterSource waterSource;
    private Boolean hasFirstAid;
}
