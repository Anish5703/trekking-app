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

    private Integer id;
    private Integer gpxSegmentId;
    private Double latitude;
    private Double longitude;
    private Integer localSequence;
    private Integer globalSequence;
    private Double elevation;
    private Boolean isDeleted;
    private TrackPointStatus status;
}
