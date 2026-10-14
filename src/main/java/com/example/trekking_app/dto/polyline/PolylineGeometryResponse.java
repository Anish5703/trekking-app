package com.example.trekking_app.dto.polyline;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolylineGeometryResponse {

    @JsonProperty("type")
    private String type;

    @JsonProperty("coordinates")
    private Object coordinates;

    @JsonProperty("precision")
    private Integer precision;
}