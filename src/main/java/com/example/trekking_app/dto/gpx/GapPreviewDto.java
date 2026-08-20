package com.example.trekking_app.dto.gpx;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GapPreviewDto {
    private Integer precedingTpId;
    private Integer followingTpId;
    private List<Double> from; // [lon, lat]
    private List<Double> to;   // [lon, lat]
    private double distanceMeters;
}