package com.example.trekking_app.service.overpass;

import com.example.trekking_app.dto.poi.ExternalPoiDto;
import com.example.trekking_app.model.POIType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExternalPoiServiceTest {
    @Mock
    private OverpassQueryBuilder queryBuilder;
    @Mock
    private OverpassClient overpassClient;
    @Mock
    private OsmTagMapper osmTagMapper;
    private ObjectMapper objectMapper ;
    @InjectMocks
    private ExternalPoiService externalPoiService;

    @Test
    void testFetchForBoundingBox_Success () throws Exception{
        double minLat = 25.0 , minLng = 85.0, maxLat=26.0,maxLng =86.0;
        String dummyQuery = "overpass query";
        String rawJson = "{\"elements\":[{\"type\":\"node\",\"id\":1,\"lat\":25.5,\"lon\":85.5,\"tags\":{\"name\":\"Mountain Lodge\",\"tourism\":\"hotel\"}}]}";

        objectMapper = new ObjectMapper();
        JsonNode responseNode = objectMapper.readTree(rawJson);

        when(queryBuilder.build(minLat,minLng,maxLat,maxLng)).thenReturn(dummyQuery);
        when(overpassClient.query(dummyQuery)).thenReturn(responseNode);
        when(osmTagMapper.map(any(JsonNode.class))).thenReturn(POIType.HOTEL);

        List<ExternalPoiDto> result = externalPoiService.fetchForBoundingBox(minLat,minLng,maxLat,maxLng);

        assertNotNull(result);
        assertEquals(1,result.size());
        assertEquals(POIType.HOTEL,result.get(0).type());
        assertEquals("Mountain Lodge",result.get(0).name());
    }

    @Test
    void testFetchForBoundingBox_ReturnsEmptyList() {

        when(queryBuilder.build(anyDouble(),anyDouble(),anyDouble(),anyDouble())).thenReturn("ql");
        when(overpassClient.query(anyString())).thenReturn(null);

        List<ExternalPoiDto> result = externalPoiService.fetchForBoundingBox(23.0,80.0,24.0,81.0);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetForRoute_Success () {
        List<double[]> routePoints = List.of(new double[]{27.123,85.123}, new double[]{27.456,85.445});

        when(queryBuilder.build(anyDouble(),anyDouble(),anyDouble(),anyDouble())).thenReturn("query");

        objectMapper = new ObjectMapper();
        JsonNode emptyResponse = objectMapper.createObjectNode().set("elements",objectMapper.createArrayNode());
        when(overpassClient.query(anyString())).thenReturn(emptyResponse);

        List<ExternalPoiDto> result = externalPoiService.getForRoute(routePoints);

        assertNotNull(result);
        verify(queryBuilder).build(eq(27.12),eq(85.12),eq(27.46),eq(85.45));
    }

    @Test
    void testGetForRoute_ReturnsEmptyList() {
        List<double[]> routePoints= List.of();

        List<ExternalPoiDto> result = externalPoiService.getForRoute(routePoints);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
