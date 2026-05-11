package com.example.trekking_app.dto.geoJson;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class GeoJsonGeometry {

    @JsonProperty("type")
    private String type;

    @JsonProperty("coordinates")
    private Object coordinates;
}
