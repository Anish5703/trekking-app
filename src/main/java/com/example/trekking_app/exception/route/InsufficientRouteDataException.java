package com.example.trekking_app.exception.route;

import lombok.Getter;
import java.util.List;

@Getter
public class InsufficientRouteDataException extends RuntimeException {
    private final Integer routeId;
    private final List<String> missing;

    public InsufficientRouteDataException(Integer routeId, List<String> missing) {
        super("Route " + routeId + " is missing data required for grading: " + missing);
        this.routeId = routeId;
        this.missing = missing;
    }
}