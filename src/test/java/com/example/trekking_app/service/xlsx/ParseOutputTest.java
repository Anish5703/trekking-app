package com.example.trekking_app.service.xlsx;

import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.TrailSegment;
import com.example.trekking_app.entity.WayPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseOutputTest {

    private WayPoint wp1;
    private WayPoint wp2;
    private WayPoint wp3;
    private POI poi;

    @BeforeEach
    void setUp() {
        wp1 = new WayPoint();
        wp1.setId(1);

        wp2 = new WayPoint();
        wp2.setId(2);

        wp3 = new WayPoint();
        wp3.setId(3);

        poi = new POI();
        poi.setId(4);
    }

    @Test
    void testGpxSegmentCount_EmptyRawRows_ReturnsOne() {
        ParseOutput output = new ParseOutput(List.of(), List.of(), List.of(), List.of(), List.of());

        assertEquals(1, output.gpxSegmentCount());
    }

    @Test
    void testGpxSegmentCount_ReturnsMaxIndexPlusOne() {
        List<XlsxParserResult> rawRows = List.of(
                XlsxParserResult.ofPOI(wp1, poi, 0),
                XlsxParserResult.ofPOI(wp2, poi, 2),
                XlsxParserResult.ofPOI(wp3, poi, 1)
        );
        ParseOutput output = new ParseOutput(rawRows, List.of(), List.of(), List.of(), List.of());

        assertEquals(3, output.gpxSegmentCount());
    }

    @Test
    void testWayPointsByGpxOrderIndex_GroupsCorrectly() {
        List<XlsxParserResult> rawRows = List.of(
                XlsxParserResult.ofPOI(wp1, poi, 1),
                XlsxParserResult.ofPOI(wp2, poi, 1),
                XlsxParserResult.ofPOI(wp3, poi, 2)
        );
        ParseOutput output = new ParseOutput(rawRows, List.of(), List.of(), List.of(), List.of());

        Map<Integer, List<WayPoint>> grouped = output.wayPointsByGpxOrderIndex();

        assertEquals(2, grouped.get(1).size());
        assertEquals(1, grouped.get(2).size());
        assertTrue(grouped.get(1).containsAll(List.of(wp1, wp2)));
    }

    @Test
    void testSegmentsByGpxOrderIndex_OnlyIncludesPairedSegments() {
        TrailSegment paired = new TrailSegment();
        paired.setStartWaypoint(wp1);
        paired.setEndWaypoint(wp2);

        TrailSegment unpaired = new TrailSegment();
        unpaired.setStartWaypoint(null);
        unpaired.setEndWaypoint(wp3);

        List<XlsxParserResult> rawRows = List.of(
                XlsxParserResult.ofTrailSegment(wp1, paired, 1),
                XlsxParserResult.ofTrailSegment(wp3, unpaired, 1)
        );
        ParseOutput output = new ParseOutput(rawRows, List.of(), List.of(), List.of(), List.of());

        Map<Integer, List<TrailSegment>> grouped = output.segmentsByGpxOrderIndex();

        assertEquals(1, grouped.get(1).size());
        assertEquals(paired, grouped.get(1).get(0));
    }

    @Test
    void testWayPoints_ReturnsUnmodifiableList() {
        ParseOutput output = new ParseOutput(List.of(), List.of(wp1), List.of(), List.of(), List.of());

        assertThrows(UnsupportedOperationException.class, () -> output.wayPoints().add(wp2));
    }
}