package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.waypoint.WayPointRequest;
import com.example.trekking_app.dto.waypoint.WayPointResponse;
import com.example.trekking_app.entity.WayPoint;
import com.example.trekking_app.model.WayPointStatus;

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
    public WayPoint toUpdateWayPoint(WayPoint wp , WayPointRequest wpr)
    {
        wp.setLongitude(wpr.getLongitude());
        wp.setLatitude(wpr.getLatitude());
        wp.setElevation(wpr.getElevation());
        wp.setStatus(wpr.getStatus());
        wp.setIsDeleted(wp.getStatus().equals(WayPointStatus.SOFT_DELETED));
        return wp;
    }
}
