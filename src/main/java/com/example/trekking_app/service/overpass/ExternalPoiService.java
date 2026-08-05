package com.example.trekking_app.service.overpass;

import com.example.trekking_app.dto.poi.ExternalPoiDto;
import com.example.trekking_app.model.POIType;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalPoiService {

    private final OverpassQueryBuilder queryBuilder;
    private final OverpassClient overpassClient;
    private final OsmTagMapper tagMapper;

    @Cacheable(value = "externalPois",
            key = "#minLat + '_' + #minLng + '_' + #maxLat + '_' + #maxLng",
            unless = "#result == null || #result.isEmpty()")
    public List<ExternalPoiDto> fetchForBoundingBox(double minLat, double minLng, double maxLat, double maxLng) {
        log.info("fetchForBoundingBox called: [{}, {}, {}, {}]", minLat, minLng, maxLat, maxLng);

        String ql = queryBuilder.build(minLat, minLng, maxLat, maxLng);
        JsonNode response = overpassClient.query(ql);

        if (response == null) {
            log.warn("Overpass response was null");
            return List.of();
        }
        if (!response.has("elements")) {
            log.warn("Overpass response missing 'elements' field. Full response: {}", response);
            return List.of();
        }

        int totalElements = response.get("elements").size();
        log.info("Overpass returned {} raw elements", totalElements);

        List<ExternalPoiDto> results = new ArrayList<>();
        int skippedNoType = 0;

        for (JsonNode element : response.get("elements")) {
            JsonNode tags = element.get("tags");
            POIType type = tagMapper.map(tags);
            if (type == null) { skippedNoType++; continue; }

            String name = tags.has("name") ? tags.get("name").asText() : type.name();
            double lat = element.get("lat").asDouble();
            double lon = element.get("lon").asDouble();
            results.add(new ExternalPoiDto(name, type, lat, lon));
        }

        log.info("Mapped {} POIs, skipped {} (no matching POIType)", results.size(), skippedNoType);
        return results;
    }

    public List<ExternalPoiDto> getForRoute(List<double[]> routePoints) {
        log.info("getForRoute called with {} route points", routePoints.size());
        if (routePoints.isEmpty()) {
            log.warn("routePoints is empty — route.getPath() may be null");
            return List.of();
        }

        double minLat = routePoints.stream().mapToDouble(p -> p[0]).min().orElseThrow();
        double maxLat = routePoints.stream().mapToDouble(p -> p[0]).max().orElseThrow();
        double minLng = routePoints.stream().mapToDouble(p -> p[1]).min().orElseThrow();
        double maxLng = routePoints.stream().mapToDouble(p -> p[1]).max().orElseThrow();

        double grid = 0.01;
        double snMinLat = Math.floor(minLat / grid) * grid;
        double snMinLng = Math.floor(minLng / grid) * grid;
        double snMaxLat = Math.ceil(maxLat / grid) * grid;
        double snMaxLng = Math.ceil(maxLng / grid) * grid;

        log.info("Computed bbox (snapped): [{}, {}, {}, {}]", snMinLat, snMinLng, snMaxLat, snMaxLng);

        return fetchForBoundingBox(snMinLat, snMinLng, snMaxLat, snMaxLng);
    }
}