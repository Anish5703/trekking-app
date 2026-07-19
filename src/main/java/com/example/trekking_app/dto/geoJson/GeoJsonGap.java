package com.example.trekking_app.dto.geoJson;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class GeoJsonGap {
    private List<Double> from;
    private List<Double> to;
    private Double distanceMeters;
}