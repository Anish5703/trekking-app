package com.example.trekking_app.dto.trackpoint;

import com.example.trekking_app.model.TrackPointStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrackPointResponse {

    private int id;
    private int gpxSegmentId;
    private double latitude;
    private double longitude;
    private int localSequence;
    private int globalSequence;
    private double elevation;
    private boolean isDeleted;
    private TrackPointStatus status;
}
