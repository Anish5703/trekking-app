package com.example.trekking_app.dto.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class GpxImportResponse {

    private int routeId;
    private String routeName;
    private int numberOfTrackPoints;
    private String addedBy;
}
