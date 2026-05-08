package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.trackpoint.TrackPointResponse;
import com.example.trekking_app.entity.TrackPoint;

public class TrackPointMapper {

    public TrackPointResponse toTrackPointResponse(TrackPoint trackPoint)
    {
          return TrackPointResponse.builder()
                  .id(trackPoint.getId())
                  .gpxSegmentId(trackPoint.getGpxSegment().getId())
                  .latitude(trackPoint.getLatitude())
                  .longitude(trackPoint.getLongitude())
                  .localSequence(trackPoint.getLocalSequence())
                  .globalSequence(trackPoint.getGlobalSequence())
                  .elevation(trackPoint.getElevation())
                  .isDeleted(trackPoint.getIsDeleted())
                  .status(trackPoint.getStatus())
                  .build();
    }

}
