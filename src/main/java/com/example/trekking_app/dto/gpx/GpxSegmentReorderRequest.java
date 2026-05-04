package com.example.trekking_app.dto.gpx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpxSegmentReorderRequest {

    List<Integer> segmentIdsInOrder;
}
