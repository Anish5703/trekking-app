package com.example.trekking_app.model;

import com.example.trekking_app.exception.route.FileParsingFailedException;

public class CloudinaryFolders {
    public static final String USERS = "trekking/users";
    public static final String ROUTES = "trekking/routes";
    public static final String DESTINATIONS = "trekking/destinations";
    public static final String POIS = "trekking/pois";
    public static final String ACCOMMODATIONS = "trekking/accommodation";
    public static final String TRAIL_SEGMENTS = "trekking/trailSegments";

    public static String resolvePath(EntityType entityType, Integer entityId)
    {
           String directory = null;
           switch(entityType)
           {
               case ROUTE -> directory = ROUTES+"/"+entityId;
               case POI -> directory =  POIS+"/"+entityId;
               case DESTINATION -> directory =  DESTINATIONS+"/"+entityId;
               case ACCOMMODATION ->  directory = ACCOMMODATIONS+"/"+entityId;
               case TRAIL_SEGMENT -> directory = TRAIL_SEGMENTS+"/"+entityId;
               case USER ->  directory = USERS+"/"+entityId;

               default -> throw new FileParsingFailedException("Failed to upload image unknown entity type");
           }
           return directory;
    }
}

