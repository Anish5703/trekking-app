package com.example.trekking_app.dto.trackpoint;

import com.example.trekking_app.model.TrackPointStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class TrackPointRequest {

    private Double longitude;
    private Double latitude;
    private Double elevation;
    private Integer localSequence;
    private TrackPointStatus status;
    private Boolean isDeleted;
}
