package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.trackpoint.TrackPointDetails;
import com.example.trekking_app.entity.TrackPoint;

public class TrackPointMapper {

    public TrackPointDetails toTrackPointDetails(TrackPoint trackPoint)
    {
          return TrackPointDetails.builder()
                  .id(trackPoint.getId())
                  .lat(trackPoint.getLatitude())
                  .lon(trackPoint.getLongitude())
                  .sequenceOrder(trackPoint.getGlobal_sequence())
                  .elevation(trackPoint.getElevation())
                  .build();
    }
}
