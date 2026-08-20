package com.example.trekking_app.service.overpass;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OverpassQueryBuilderTest {

    private OverpassQueryBuilder queryBuilder;

    @BeforeEach
    void setUp() {
        queryBuilder = new OverpassQueryBuilder();
    }

    @Test
    void testBuild_StartsWithQueryHeader() {
        String query = queryBuilder.build(27.7, 86.5, 28.2, 87.0);

        assertTrue(query.startsWith("[out:json][timeout:25];("));
    }

    @Test
    void testBuild_EndsWithQueryFooter() {
        String query = queryBuilder.build(27.7, 86.5, 28.2, 87.0);

        assertTrue(query.endsWith(");out body;"));
    }

    @Test
    void testBuild_ContainsNodeQueryForEachAmenityAndTourismTag() {
        String query = queryBuilder.build(27.7, 86.5, 28.2, 87.0);

        assertTrue(query.contains("node[\"amenity\"=\"hospital\"]"));
        assertTrue(query.contains("node[\"tourism\"=\"viewpoint\"]"));
        assertTrue(query.contains("node[\"natural\"=\"peak\"]"));
    }

    @Test
    void testBuild_ContainsGenericShopQuery() {
        String query = queryBuilder.build(27.7, 86.5, 28.2, 87.0);

        assertTrue(query.contains("node[\"shop\"]"));
    }

    @Test
    void testBuild_FormatsCoordinatesWithSixDecimalPlaces() {
        String query = queryBuilder.build(27.7, 86.5, 28.2, 87.0);

        assertTrue(query.contains("(27.700000,86.500000,28.200000,87.000000);"));
    }

}