package com.example.trekking_app.service.xlsx;

import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.TrailSegment;
import com.example.trekking_app.entity.WayPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XlsxParserResultTest {

    private WayPoint wayPoint;

    @BeforeEach
    void setUp() {
        wayPoint = new WayPoint();
        wayPoint.setId(1);
    }

    @Test
    void testOfTrailSegment_SetsOutcomeTypeTrailSegment() {
        TrailSegment trailSegment = new TrailSegment();
        XlsxParserResult result = XlsxParserResult.ofTrailSegment(wayPoint, trailSegment, 1);

        assertTrue(result.isTrailSegment());
        assertFalse(result.isAccommodation());
        assertFalse(result.isPOI());
        assertEquals(trailSegment, result.getTrailSegment());
        assertEquals(wayPoint, result.getWayPoint());
        assertEquals(1, result.getGpxOrderIndex());
    }

    @Test
    void testOfPOI_SetsOutcomeTypePOI() {
        POI poi = new POI();
        XlsxParserResult result = XlsxParserResult.ofPOI(wayPoint, poi, 2);

        assertTrue(result.isPOI());
        assertFalse(result.isAccommodation());
        assertFalse(result.isTrailSegment());
        assertEquals(poi, result.getPoi());
    }

    @Test
    void testOfAccommodation_SetsOutcomeTypeAccommodation() {
        Accommodation accommodation = new Accommodation();
        XlsxParserResult result = XlsxParserResult.ofAccommodation(wayPoint, accommodation, 3);

        assertTrue(result.isAccommodation());
        assertFalse(result.isPOI());
        assertFalse(result.isTrailSegment());
        assertEquals(accommodation, result.getAccommodation());
    }

    @Test
    void testConstructor_AllNull_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new XlsxParserResult(null, null, null, wayPoint, 1));
    }

    @Test
    void testConstructor_PoiTakesPrecedenceOverAccommodation() {
        POI poi = new POI();
        Accommodation accommodation = new Accommodation();
        XlsxParserResult result = new XlsxParserResult(poi, null, accommodation, wayPoint, 1);

        assertTrue(result.isPOI());
        assertFalse(result.isAccommodation());
    }

    @Test
    void testConstructor_AccommodationTakesPrecedenceOverTrailSegment() {
        Accommodation accommodation = new Accommodation();
        TrailSegment trailSegment = new TrailSegment();
        XlsxParserResult result = new XlsxParserResult(null, trailSegment, accommodation, wayPoint, 1);

        assertTrue(result.isAccommodation());
        assertFalse(result.isTrailSegment());
    }
}