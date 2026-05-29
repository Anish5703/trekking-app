package com.example.trekking_app.dto.poi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class XlsxPoiRow {

    private Integer waypointNumber;
    private String trailPath;
    private String startOrEnd;
    private String importantStop;
    private String name;
    private  Map<String,String> extras;
}
