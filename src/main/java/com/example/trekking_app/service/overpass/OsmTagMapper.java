package com.example.trekking_app.service.overpass;

import com.example.trekking_app.model.POIType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class OsmTagMapper {

    public POIType map(JsonNode tags) {
        if (tags == null) return null;
        String amenity = text(tags, "amenity");
        String tourism = text(tags, "tourism");
        String natural = text(tags, "natural");
        String shop = text(tags, "shop");

        if ("school".equals(amenity)) return POIType.SCHOOL;
        if ("hospital".equals(amenity)) return POIType.HOSPITAL;
        if ("clinic".equals(amenity) || "pharmacy".equals(amenity)) return POIType.HEALTH_POST;
        if ("drinking_water".equals(amenity)) return POIType.DRINKING_WATER;
        if ("toilets".equals(amenity)) return POIType.RESTROOM;
        if ("atm".equals(amenity)) return POIType.ATM;
        if ("police".equals(amenity)) return POIType.POLICE_STATION;
        if ("place_of_worship".equals(amenity)) return POIType.TEMPLE;
        if ("guest_house".equals(tourism) || "hotel".equals(tourism)) return POIType.HOTEL;
        if ("viewpoint".equals(tourism)) return POIType.VIEWPOINT;
        if ("camp_site".equals(tourism)) return POIType.CAMP_SPOT;
        if ("peak".equals(natural)) return POIType.PEAK;
        if (shop != null) return POIType.WORKSHOP;
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null ? null : v.asText();
    }
}