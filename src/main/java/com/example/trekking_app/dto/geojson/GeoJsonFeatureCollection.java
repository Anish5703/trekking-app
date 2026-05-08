package com.example.trekking_app.dto.geojson;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class GeoJsonFeatureCollection {

    @JsonProperty("type")
    private String type;
    @JsonProperty("features")
    private List<GeoJsonFeature> features;

}
