package com.example.trekking_app.service.overpass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class OverpassQueryBuilder {

    private static final List<String> TAGS = List.of(
            "amenity=school", "amenity=hospital", "amenity=clinic", "amenity=pharmacy",
            "amenity=drinking_water", "amenity=toilets", "amenity=atm", "amenity=police",
            "amenity=place_of_worship", "tourism=guest_house", "tourism=hotel",
            "tourism=viewpoint", "tourism=camp_site", "natural=peak"
    );

    public String build(double minLat, double minLng, double maxLat, double maxLng) {
        StringBuilder sb = new StringBuilder("[out:json][timeout:25];(");
        String bbox = String.format(Locale.US, "(%f,%f,%f,%f);", minLat, minLng, maxLat, maxLng);
        for (String tag : TAGS) {
            String[] kv = tag.split("=");
            sb.append("node[\"").append(kv[0]).append("\"=\"").append(kv[1]).append("\"]").append(bbox);
        }
        sb.append("node[\"shop\"]").append(bbox);
        sb.append(");out body;");
        log.info("Built Overpass query: {}", sb);
        return sb.toString();
    }
}