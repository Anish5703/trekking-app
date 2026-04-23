package com.example.trekking_app.dto.trackpoint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrackPointDetails {

    private int id;
    private double lat;
    private double lon;
    private int sequenceOrder;
    private double elevation;
}
