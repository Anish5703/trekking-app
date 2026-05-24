package com.example.trekking_app.dto.waypoint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class WayPointImportResponse {
    private Integer routeId;
    private Integer totalWayPoints;
}
