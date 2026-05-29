package com.example.trekking_app.service.xlsx;

import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.TrailSegment;
import com.example.trekking_app.entity.WayPoint;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParseOutput {

    private final List<XlsxParserResult> rawRows;
    private final List<WayPoint>             wayPoints;
    private final List<POI>                  pois;
    private final List<Accommodation>        accommodations;
    private final List<TrailSegment> trailSegments;

    ParseOutput(List<XlsxParserResult> rawRows,
                List<WayPoint>      wayPoints,
                List<POI>           pois,
                List<Accommodation> accommodations,
                List<TrailSegment> trailSegments) {
        this.rawRows        = Collections.unmodifiableList(rawRows);
        this.wayPoints      = Collections.unmodifiableList(wayPoints);
        this.pois           = Collections.unmodifiableList(pois);
        this.accommodations = Collections.unmodifiableList(accommodations);
        this.trailSegments = Collections.unmodifiableList(trailSegments);
    }

    /** All waypoints in sheet order. */
    public List<WayPoint>             wayPoints()      { return wayPoints; }
    /** All plain POI rows (excludes Accommodation subtypes). */
    public List<POI>                  pois()           { return pois; }
    /** All Accommodation rows (Hotel / Tea House). */
    public List<Accommodation>        accommodations() { return accommodations; }
    /** All fully-paired TrailSegments (Start↔End). */
    public List<TrailSegment> trailSegments()       { return trailSegments; }
    /** Every raw row result in sheet order — use for GpxSegment wiring. */
    public List<XlsxParserResult> rawRows()        { return rawRows; }

    /**
     * Number of distinct GPX segments detected (= highest gpxOrderIndex + 1).
     * Should equal the number of GpxSegment entities for this route.
     */
    public int gpxSegmentCount() {
        return rawRows.stream()
                .mapToInt(XlsxParserResult::getGpxOrderIndex)
                .max().orElse(0) + 1;
    }

    /** Waypoints grouped by gpxOrderIndex — convenience for bulk FK assignment. */
    public Map<Integer, List<WayPoint>> wayPointsByGpxOrderIndex() {
        return rawRows.stream().collect(Collectors.groupingBy(
                XlsxParserResult::getGpxOrderIndex,
                Collectors.mapping(XlsxParserResult::getWayPoint,
                        Collectors.toList())));
    }

    /** TrailSegments grouped by gpxOrderIndex. */
    public Map<Integer, List<TrailSegment>> segmentsByGpxOrderIndex() {
        return rawRows.stream()
                .filter(XlsxParserResult::isTrailSegment)
                .filter(r -> r.getTrailSegment().getStartWaypoint() != null) // paired only
                .collect(Collectors.groupingBy(
                        XlsxParserResult::getGpxOrderIndex,
                        Collectors.mapping(XlsxParserResult::getTrailSegment,
                                Collectors.toList())));
    }

}
