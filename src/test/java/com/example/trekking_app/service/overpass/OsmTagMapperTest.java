package com.example.trekking_app.service.overpass;

import com.example.trekking_app.model.POIType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OsmTagMapperTest {

    private OsmTagMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mapper = new OsmTagMapper();
        objectMapper = new ObjectMapper();
    }

    private JsonNode tags(Map<String, String> entries) {
        ObjectNode node = objectMapper.createObjectNode();
        entries.forEach(node::put);
        return node;
    }

    @Test
    void testMap_NullTags_ReturnsNull() {
        assertNull(mapper.map(null));
    }

    @Test
    void testMap_AmenitySchool_ReturnsSchool() {
        assertEquals(POIType.SCHOOL, mapper.map(tags(Map.of("amenity", "school"))));
    }

    @Test
    void testMap_AmenityHospital_ReturnsHospital() {
        assertEquals(POIType.HOSPITAL, mapper.map(tags(Map.of("amenity", "hospital"))));
    }

    @Test
    void testMap_AmenityClinic_ReturnsHealthPost() {
        assertEquals(POIType.HEALTH_POST, mapper.map(tags(Map.of("amenity", "clinic"))));
    }

    @Test
    void testMap_AmenityPharmacy_ReturnsHealthPost() {
        assertEquals(POIType.HEALTH_POST, mapper.map(tags(Map.of("amenity", "pharmacy"))));
    }

    @Test
    void testMap_AmenityDrinkingWater_ReturnsDrinkingWater() {
        assertEquals(POIType.DRINKING_WATER, mapper.map(tags(Map.of("amenity", "drinking_water"))));
    }

    @Test
    void testMap_AmenityToilets_ReturnsRestroom() {
        assertEquals(POIType.RESTROOM, mapper.map(tags(Map.of("amenity", "toilets"))));
    }

    @Test
    void testMap_AmenityAtm_ReturnsAtm() {
        assertEquals(POIType.ATM, mapper.map(tags(Map.of("amenity", "atm"))));
    }

    @Test
    void testMap_AmenityPolice_ReturnsPoliceStation() {
        assertEquals(POIType.POLICE_STATION, mapper.map(tags(Map.of("amenity", "police"))));
    }

    @Test
    void testMap_AmenityPlaceOfWorship_ReturnsTemple() {
        assertEquals(POIType.TEMPLE, mapper.map(tags(Map.of("amenity", "place_of_worship"))));
    }

    @Test
    void testMap_TourismGuestHouse_ReturnsHotel() {
        assertEquals(POIType.HOTEL, mapper.map(tags(Map.of("tourism", "guest_house"))));
    }

    @Test
    void testMap_TourismHotel_ReturnsHotel() {
        assertEquals(POIType.HOTEL, mapper.map(tags(Map.of("tourism", "hotel"))));
    }

    @Test
    void testMap_TourismViewpoint_ReturnsViewpoint() {
        assertEquals(POIType.VIEWPOINT, mapper.map(tags(Map.of("tourism", "viewpoint"))));
    }

    @Test
    void testMap_TourismCampSite_ReturnsCampSpot() {
        assertEquals(POIType.CAMP_SPOT, mapper.map(tags(Map.of("tourism", "camp_site"))));
    }

    @Test
    void testMap_NaturalPeak_ReturnsPeak() {
        assertEquals(POIType.PEAK, mapper.map(tags(Map.of("natural", "peak"))));
    }

    @Test
    void testMap_ShopFieldPresent_ReturnsWorkshop() {
        assertEquals(POIType.WORKSHOP, mapper.map(tags(Map.of("shop", "bakery"))));
    }

    @Test
    void testMap_NoRecognizedField_ReturnsNull() {
        assertNull(mapper.map(tags(Map.of("highway", "path"))));
    }
}