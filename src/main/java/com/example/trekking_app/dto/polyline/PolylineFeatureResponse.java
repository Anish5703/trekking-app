package com.example.trekking_app.dto.polyline;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolylineFeatureResponse {

    private String id;

    @Builder.Default
    @JsonProperty("type")
    private String type = "Feature";

    @JsonProperty("properties")
    private Map<String, Object> properties;

    @JsonProperty("geometry")
    private PolylineGeometryResponse geometry;
}