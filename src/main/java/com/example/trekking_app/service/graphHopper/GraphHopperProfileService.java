package com.example.trekking_app.service.graphHopper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphHopperProfileService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${graphhopper.base-url}")
    private String GRAPHHOPPER_BASE_URL;

    public List<String> getAvailableProfiles() {
        List<String> profiles = new ArrayList<>();
        String url = GRAPHHOPPER_BASE_URL + "/info";

        try {
            String response = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode root = mapper.readTree(response);
            JsonNode profilesNode = root.path("profiles");

            if (profilesNode.isArray()) {
                for (JsonNode p : profilesNode) {
                    profiles.add(p.path("name").asText());
                }
            }
            log.info("Fetched GraphHopper profiles: {}", profiles);
        } catch (Exception e) {
            log.error("Failed to fetch GraphHopper profiles: {}", e.getLocalizedMessage());
        }

        return profiles;
    }
}