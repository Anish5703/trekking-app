package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.trackpoint.TrackPointInfo;
import com.example.trekking_app.dto.trackpoint.TrackPointRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointResponse;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.model.TrackPointStatus;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import javax.sound.midi.Track;

public class TrackPointMapper {

    private final GeometryFactory GF = new GeometryFactory(new PrecisionModel(),4326);


    public TrackPointResponse toTrackPointResponse(TrackPoint trackPoint)
    {
          return TrackPointResponse.builder()
                  .id(trackPoint.getId())
                  .gpxSegmentId(trackPoint.getGpxSegment().getId())
                  .latitude(trackPoint.getLatitude())
                  .longitude(trackPoint.getLongitude())
                  .localSequence(trackPoint.getLocalSequence())
                  .globalSequence(trackPoint.getGlobalSequence())
                  .elevation(trackPoint.getElevation()!=null ? trackPoint.getElevation() : 0.0)
                  .isDeleted(trackPoint.getIsDeleted())
                  .status(trackPoint.getStatus())
                  .build();
    }

    public TrackPoint toUpdateTrackPoint(TrackPoint trackPoint , TrackPointRequest trackPointRequest)
    {
        trackPoint.setLongitude(trackPointRequest.getLongitude());
        trackPoint.setLatitude(trackPointRequest.getLatitude());
        trackPoint.setElevation(trackPointRequest.getElevation()!=null ? trackPoint.getElevation() : 0.0);
        trackPoint.setStatus(trackPointRequest.getStatus());
        trackPoint.setIsDeleted(trackPoint.getStatus().equals(TrackPointStatus.SOFT_DELETED));
        Point point = GF.createPoint(new Coordinate(trackPointRequest.getLongitude(), trackPointRequest.getLatitude()));
        trackPoint.setGeom(point);
        return trackPoint;
    }

    public TrackPointInfo toTrackPointInfo(TrackPoint tp)
    {
        return TrackPointInfo.builder()
                .id(tp.getId())
                .longitude(tp.getLongitude())
                .latitude(tp.getLatitude())
                .globalSequence(tp.getGlobalSequence())
                .build();
    }

}
