package com.example.trekking_app.dto.geoJson;

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
public class GeoJsonFeatureCollection
{

    @Builder.Default
    @JsonProperty("type")
    private String type = "FeatureCollection";
    @JsonProperty("features")
    private List<GeoJsonFeature> features;
    @JsonProperty("gaps")
    private List<GeoJsonGap> gaps;
}
