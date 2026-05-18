package com.example.trekking_app.dto.poi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class POIUploadResponse {

    private Integer routeId;
    private Integer gpxSegmentId;
    private int totalRows;
    private int wayPointsUpserted;
    private int poisCreated;
    @Builder.Default
    private List<String> skipped = new ArrayList<>();
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
