package com.example.trekking_app.service.overpass;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class OverpassClient {

    private final RestTemplate restTemplate;

    @Value("${overpass.url}")
    private String overpassUrl;

    public JsonNode query(String overpassQl) {
        log.info("Calling Overpass at {}", overpassUrl);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("data", overpassQl);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(overpassUrl, request, String.class);

            log.info("Overpass HTTP status: {}", response.getStatusCode());
            log.info("Overpass raw response (first 500 chars): {}",
                    response.getBody() != null
                            ? response.getBody().substring(0, Math.min(500, response.getBody().length()))
                            : "null");

            if (response.getBody() == null) return null;
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(response.getBody());

        } catch (RestClientException e) {
            log.error("Overpass query failed - RestClientException: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Overpass query failed - unexpected error: {}", e.getMessage());
            return null;
        }
    }
}