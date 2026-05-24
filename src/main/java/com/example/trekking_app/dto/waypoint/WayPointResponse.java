package com.example.trekking_app.dto.waypoint;

import com.example.trekking_app.model.TrackPointStatus;
import com.example.trekking_app.model.WayPointStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class WayPointResponse {
    private int id;
    private int gpxSegmentId;
    private double latitude;
    private double longitude;
    private int localSequence;
    private int globalSequence;
    private double elevation;
    private boolean isDeleted;
    private WayPointStatus status;
}
