package com.example.trekking_app.dto.poi;

import com.example.trekking_app.model.POIType;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PoiRequest {

    @NonNull
    private Integer routeId;
    private String name;
    @NonNull
    private Double latitude;
    @NonNull
    private Double longitude;
    private Double elevation;
    @NonNull
    private POIType type;
    private String description;


}
