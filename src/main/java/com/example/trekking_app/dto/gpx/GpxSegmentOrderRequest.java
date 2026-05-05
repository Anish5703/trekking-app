package com.example.trekking_app.dto.gpx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GpxSegmentOrderRequest {

    @Builder.Default
    Map<Integer,Integer> segmentIdWithOrder = new HashMap<>();
}
