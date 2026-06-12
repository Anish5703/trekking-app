package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.trailSegment.TrailSegmentResponse;
import com.example.trekking_app.dto.trailSegment.TrailSegmentUpdateRequest;
import com.example.trekking_app.entity.TrailSegment;
import lombok.NonNull;


public class TrailSegmentMapper {



    public TrailSegmentResponse toTrailSegmentResponse(TrailSegment trail)
    {
        return TrailSegmentResponse.builder()
                .id(trail.getId())
                .name(trail.getName())
                .routeId(trail.getRoute().getId())
                .type(trail.getType())
                .startWaypointId(trail.getStartWaypoint().getId())
                .endWaypointId(trail.getEndWaypoint().getId())
                .color(trail.getColor())
                .gpxSegmentId(trail.getGpxSegment().getId())
                .estimatedTimeMinutes(trail.getEstimatedTimeMinutes())
                .distanceInMeter(trail.getDistanceInMeter())
                .build();
    }

    public TrailSegment toUpdateEntity(@NonNull TrailSegment trailSegment, @NonNull TrailSegmentUpdateRequest req)
    {
        trailSegment.setName(req.getName());
        trailSegment.setType(req.getType());
        trailSegment.setEstimatedTimeMinutes(req.getEstimatedTimeMinutes());
        trailSegment.setDistanceInMeter(req.getDistanceInMeter());
        return trailSegment;
    }
}
