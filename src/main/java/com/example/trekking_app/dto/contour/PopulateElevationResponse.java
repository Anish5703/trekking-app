package com.example.trekking_app.dto.contour;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PopulateElevationResponse {
    int routeId;
    int pointCount;
    int missingBefore;        // trackpoints without elevation before this call
    int elevationsFetched;    // trackpoints updated by this call
    int stillMissing;         // trackpoints still without elevation (no DEM data / invalid coordinates)
}