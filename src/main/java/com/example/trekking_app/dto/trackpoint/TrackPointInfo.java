package com.example.trekking_app.dto.trackpoint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TrackPointInfo {
    private Integer id;
    private Double longitude;
    private Double latitude;
    private Integer globalSequence;

}
