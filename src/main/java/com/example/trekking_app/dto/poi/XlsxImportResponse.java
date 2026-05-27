package com.example.trekking_app.dto.poi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class XlsxImportResponse {

    private Integer numberOfRows;
    private Integer numberOfWayPoint;
    private Integer numberOfPOI;
    private Integer numberOfAccommodation;
    private Integer numberOfTrailSegment;

}
