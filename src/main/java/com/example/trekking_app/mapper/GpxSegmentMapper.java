package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.gpx.GpxSegmentResponse;
import com.example.trekking_app.entity.GpxSegment;

public class GpxSegmentMapper {

    public GpxSegmentResponse toGpxSegmentResponse(GpxSegment gpxSegment)
    {
        return GpxSegmentResponse.builder()
                .id(gpxSegment.getId())
                .routeId(gpxSegment.getRoute().getId())
                .sourceFilename(gpxSegment.getSourceFileName())
                .orderIndex(gpxSegment.getOrderIndex())
                .trackPointCount(gpxSegment.getTrackPoints().size())
                .status(gpxSegment.getStatus())
                .minTime(gpxSegment.getRecordedAt())
                .maxTime(gpxSegment.getRecordedUntil())
                .build();
    }
}
