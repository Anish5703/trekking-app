package com.example.trekking_app.dto.geojson;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class GeoJsonFeature {

    @JsonProperty("type")
    @Builder.Default
    private String type = "Feature";

    @JsonProperty("properties")
    private Map<String,Object> properties;

    @JsonProperty("geoJsonGeometry")
    private GeoJsonGeometry geoJsonGeometry;
}
