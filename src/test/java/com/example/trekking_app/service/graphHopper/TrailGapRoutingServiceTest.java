package com.example.trekking_app.service.graphHopper;

import com.example.trekking_app.dto.geoJson.GeoJsonFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


class TrailGapRoutingServiceTest{

    private TrailGapRoutingService trailGapRoutingService;
    private RestClient restClient;
    private List<Double> from;
    private List<Double> to;
    private String bridgeId;

    @BeforeEach
    void setUp() {

        restClient = Mockito.mock(RestClient.class, Mockito.RETURNS_DEEP_STUBS);
        trailGapRoutingService = new TrailGapRoutingService();

        ReflectionTestUtils.setField(trailGapRoutingService,"restClient",restClient);
        ReflectionTestUtils.setField(trailGapRoutingService,"GRAPHHOPPER_BASE_URL","http://localhost:8000");

        from = List.of(83.3,23.3); //long, lat
        to = List.of(86.6,26.6);
        bridgeId = "bridge_111";
    }

    @Test
    void testFindBridgePath_Success(){

        String mockResponse = """
            {
              "paths": [
                {
                  "distance": 1250.5,
                  "points": {
                    "coordinates": [
                      [83.3, 23.3],
                      [85.35, 27.75],
                      [86.6, 26.6]
                    ]
                  }
                }
              ]
            }
            """;

        when(restClient.get().uri(anyString()).retrieve().body(String.class)).thenReturn(mockResponse);

        GeoJsonFeature response = trailGapRoutingService.findBridgePath(from,to,bridgeId);
        assertNotNull(response);
        assertEquals(bridgeId,response.getId());
        assertEquals("LineString",response.getGeometry().getType());
        assertEquals("trail_bridge",response.getProperties().get("type"));
        assertEquals("osm_routed",response.getProperties().get("source"));
        assertEquals(1250.5,response.getProperties().get("distanceMeters"));
    }

    @Test
    void testFindBridgePath_GraphHopperErrorPayload() {
        String mockErrorResponse = """
            {
              "message": "Connection between locations not found"
            }
            """;

        when(restClient.get().uri(anyString()).retrieve().body(String.class)).thenReturn(mockErrorResponse);

        GeoJsonFeature result = trailGapRoutingService.findBridgePath(from, to, bridgeId);

        assertNull(result, "Should return null when GraphHopper responds with an error message payload");
    }

    @Test
    void testFindBridgePath_MissingCoordinates() {
        String mockEmptyCoordsResponse = """
            {
              "paths": [
                {
                  "distance": 0,
                  "points": {
                    "coordinates": []
                  }
                }
              ]
            }
            """;

        when(restClient.get().uri(anyString()).retrieve().body(String.class)).thenReturn(mockEmptyCoordsResponse);

        GeoJsonFeature result = trailGapRoutingService.findBridgePath(from, to, bridgeId);

        assertNull(result, "Should return null when coordinates are empty");
    }

    @Test
    void testFindBridgePath_RestClientNetworkException() {
        when(restClient.get().uri(anyString()).retrieve().body(String.class)).thenThrow(new RuntimeException("Connection timed out"));

        GeoJsonFeature result = trailGapRoutingService.findBridgePath(from, to, bridgeId);

        assertNull(result, "Should catch RestClient exceptions and return null");
    }

    @Test
    void testFindBridgePath_InvalidJsonResponse() {
        String invalidJson = "BAD_JSON_RESPONSE";

        when(restClient.get().uri(anyString()).retrieve().body(String.class)).thenReturn(invalidJson);

        GeoJsonFeature result = trailGapRoutingService.findBridgePath(from, to, bridgeId);

        assertNull(result, "Should catch ObjectMapper parsing exceptions and return null");
    }
}
