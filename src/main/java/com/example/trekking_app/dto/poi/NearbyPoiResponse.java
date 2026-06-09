package com.example.trekking_app.dto.poi;

import com.example.trekking_app.model.POIType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class NearbyPoiResponse
{
    private Integer id;
    private Integer routeId;
    private String name;
    private Double latitude;
    private Double longitude;
    private Double elevation;
    private POIType type;
    private String description;
    private Double distanceMetersFromCurrent;
    private List<String> imageUrls;
}
