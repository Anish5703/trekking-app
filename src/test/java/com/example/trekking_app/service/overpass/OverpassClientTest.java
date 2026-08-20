package com.example.trekking_app.service.overpass;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class OverpassClientTest {

    @Mock
    private RestTemplate restTemplate;
    @InjectMocks
    private OverpassClient overpassClient;
    private final String OVERPASS_URL = "http://overpass/api";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(overpassClient,"overpassUrl",OVERPASS_URL);
    }

    @Test
    void testQuery_Success_ReturnsParsedJsonNode(){
        String responseBody = "{\"elements\":[{\"lat\":27.7000,\"lon\":83.4500}]}";
        ResponseEntity<String> response = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.postForEntity(eq(OVERPASS_URL),any(),eq(String.class))).thenReturn(response);

        JsonNode result = overpassClient.query("query");
        assertNotNull(result);
        assertTrue(result.has("elements"));
        assertEquals(1,result.get("elements").size());
    }

    @Test
    void testQuery_ReturnsEmptyJson() {
        ResponseEntity<String> response = new ResponseEntity<>(HttpStatus.OK);

        when(restTemplate.postForEntity(eq(OVERPASS_URL),any(),eq(String.class))).thenReturn(response);
        JsonNode result = overpassClient.query("query");
        assertNull(result);
    }

    @Test
    void testQuery_RestClientException_ReturnsNull(){

        when(restTemplate.postForEntity(eq(OVERPASS_URL),any(),eq(String.class))).thenThrow(new RestClientException("connection refused"));

        JsonNode result = overpassClient.query("query");
        assertNull(result);
    }

    @Test
    void testQuery_MalformedJson_ReturnsNull(){
        ResponseEntity<String> response = new ResponseEntity<>("not a json",HttpStatus.OK);

        when(restTemplate.postForEntity(eq(OVERPASS_URL),any(),eq(String.class))).thenReturn(response);

        JsonNode result = overpassClient.query("query");
        assertNull(result);
    }
}
