package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.waypoint.WayPointResponse;
import com.example.trekking_app.entity.WayPoint;

public class WayPointMapper {


    public WayPointResponse toWayPointResponse(WayPoint wayPoint)
    {
        return WayPointResponse.builder()
                .id(wayPoint.getId())
                .gpxSegmentId(wayPoint.getGpxSegment().getId())
                .latitude(wayPoint.getLatitude())
                .longitude(wayPoint.getLongitude())
                .localSequence(wayPoint.getLocalSequence())
                .globalSequence(wayPoint.getGlobalSequence())
                .elevation(wayPoint.getElevation())
                .isDeleted(wayPoint.getIsDeleted())
                .status(wayPoint.getStatus())
                .build();
    }
}
