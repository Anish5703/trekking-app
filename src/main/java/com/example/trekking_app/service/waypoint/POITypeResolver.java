package com.example.trekking_app.service.waypoint;

import com.example.trekking_app.dto.poi.XlsxPoiRow;
import com.example.trekking_app.model.POIType;
import org.springframework.stereotype.Service;

import java.util.Locale;


@Service
public class POITypeResolver {

    public POIType resolve(XlsxPoiRow row) {
            String trail = norm(row.getTrailPath());
            String stop = norm(row.getImportantStop());
            String name = norm(row.getName());

            // 1. explicit infrastructure markers via Trail Path
            if (trail.contains("water")) return POIType.WATER_SOURCE;
            if (trail.contains("stair")) return POIType.STAIRS;

            // 2. explicit stop type
            if (stop.contains("restroom") || stop.contains("toilet")) return POIType.RESTROOM;
            if (stop.contains("trash")) return POIType.TRASH_CAN;
            if (stop.contains("checkpoint") || stop.contains("check post")) return POIType.CHECKPOINT;
            if (stop.contains("view")) return POIType.VIEWPOINT;
            if (stop.contains("health")) return POIType.HEALTH_POST;
            if (stop.contains("hospital")) return POIType.HOSPITAL;

            // 3. infer from the name
            if (containsAny(name, "hotel", "lodge", "guest house", "guesthouse")) return POIType.HOTEL;
            if (containsAny(name, "tea house", "teahouse", "tea-house")) return POIType.TEA_HOUSE;
            if (containsAny(name, "mandir", "temple", "ashram", "stupa", "gompa")) return POIType.TEMPLE;
            if (containsAny(name, "viewpoint", "view point")) return POIType.VIEWPOINT;
            if (containsAny(name, "well", "tap", "spring", "river")) return POIType.WATER_SOURCE;
            if (containsAny(name,"drinking water")) return POIType.DRINKING_WATER;
            if(containsAny(name,"non-verified drinking water")) return POIType.NON_VERIFIED_DRINKING_WATER;
            if (containsAny(name, "health post", "clinic")) return POIType.HEALTH_POST;
            if(containsAny(name,"camping spot","camping site")) return POIType.CAMP_SPOT;
            if(containsAny(name,"information board")) return POIType.INFORMATION_BOARD;
            if(containsAny(name,"police station")) return POIType.POLICE_STATION;
            if(containsAny(name,"atm")) return POIType.ATM;
            if(containsAny(name,"restroom" , "toilet","washroom")) return POIType.RESTROOM;

            // 4. fallback
            return POIType.OTHER;

    }

        private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

        private static boolean containsAny(String haystack, String... needles) {
        for (String n : needles) if (haystack.contains(n)) return true;
        return false;
    }

}
