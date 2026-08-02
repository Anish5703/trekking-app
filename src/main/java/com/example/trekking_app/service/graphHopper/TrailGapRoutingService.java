package com.example.trekking_app.service.graphHopper;
import com.example.trekking_app.dto.geoJson.GeoJsonFeature;
import com.example.trekking_app.dto.geoJson.GeoJsonGeometry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.Console;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrailGapRoutingService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${graphhopper.base-url}")
    private  String GRAPHHOPPER_BASE_URL;
    private static final String PROFILE = "foot";

    /**
     * Finds an OSM-based path connecting two gap endpoints.
     * from/to are [lon, lat] pairs, matching your GeoJsonGap format.
     */
    public GeoJsonFeature findBridgePath(List<Double> from, List<Double> to, String bridgeId) {
        log.info("GRAPHHOPPER_BASE_URL='{}'", GRAPHHOPPER_BASE_URL);
        String url = String.format(
                "%s/route?point=%f,%f&point=%f,%f&profile=%s&points_encoded=false",
                GRAPHHOPPER_BASE_URL,
                from.get(1), from.get(0),   // GraphHopper wants lat,lon
                to.get(1), to.get(0),
                PROFILE
        );
        log.info("searching trail using graph hopper from {} , {} to  {} , {}",from.get(0), from.get(1),
                to.get(0), to.get(1));

        String response;
        try {
            response = restClient.get().uri(url).retrieve().body(String.class);
        } catch (Exception e) {
            log.error("rest client error : "+e.getLocalizedMessage());
            return null; // network/4xx/5xx error
        }

        try {
            JsonNode root = mapper.readTree(response);

            if (root.has("message")) {
                // GraphHopper error payload, e.g. "Connection between locations not found"
                log.error("GraphHopper error payload, Connection between locations not found");
                return null;
            }

            JsonNode coordsNode = root.path("paths").get(0).path("points").path("coordinates");
            if (coordsNode.isMissingNode() || !coordsNode.isArray() || coordsNode.isEmpty()) {
                log.error("Coordinates  between locations not found");

                return null;
            }

            List<List<Double>> coords = new ArrayList<>();
            for (JsonNode point : coordsNode) {
                coords.add(List.of(point.get(0).asDouble(), point.get(1).asDouble()));
            }

            Map<String, Object> props = new LinkedHashMap<>();
            props.put("type", "trail_bridge");
            props.put("source", "osm_routed");
            props.put("distanceMeters", root.path("paths").get(0).path("distance").asDouble());

            log.info("found trail using graph hopper from {} , {} to {} , {} }",from.get(1), from.get(0),
                    to.get(1), to.get(0));
            return GeoJsonFeature.builder()
                    .id(bridgeId)
                    .geometry(new GeoJsonGeometry("LineString", coords))
                    .properties(props)
                    .build();

        } catch (Exception e) {
            log.error("error thrown in graph hopper service : "+e.getLocalizedMessage());
            return null;
        }
    }
}
