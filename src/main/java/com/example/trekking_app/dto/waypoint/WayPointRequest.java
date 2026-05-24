package com.example.trekking_app.dto.waypoint;

import com.example.trekking_app.model.WayPointStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class WayPointRequest
{
    private double latitude;
    private double longitude;
    private double elevation;
    private WayPointStatus status;
}

