package com.example.trekking_app.dto.poi;

import com.example.trekking_app.model.POIType;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PoiResponse {

    private Integer id;
    private Integer routeId;
    private String name;
    private Double latitude;
    private Double longitude;
    private Double elevation;
    private POIType type;
    private String description;


}