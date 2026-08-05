package com.example.trekking_app.dto.poi;

import com.example.trekking_app.model.POIType;
import java.io.Serializable;

public record ExternalPoiDto(String name, POIType type, double lat, double lon) implements Serializable {}