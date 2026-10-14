package com.example.trekking_app.dto.polyline;

import com.example.trekking_app.dto.geoJson.GeoJsonGap;
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
public class PolylineFeatureCollectionResponse {

    @Builder.Default
    @JsonProperty("type")
    private String type = "FeatureCollection";

    @Builder.Default
    @JsonProperty("encoding")
    private String encoding = "google-polyline";

    @Builder.Default
    @JsonProperty("precision")
    private Integer precision = 6;

    @JsonProperty("features")
    private List<PolylineFeatureResponse> features;

    @JsonProperty("gaps")
    private List<GeoJsonGap> gaps;
}