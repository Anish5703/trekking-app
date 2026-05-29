package com.example.trekking_app.service.xlsx;


import java.util.List;

public class XlsxParserColumnDependency {


    public static final String WAYPOINT_NUMBER    = "Waypoint Number";
    public static final String TRAIL_PATH         = "Trail Path";
    public static final String START_OR_END       = "Start or End";
    public static final String IMPORTANT_STOPS    = "Important Stops";
    public static final String OTHER              = "Other";

    // ── LEVEL 1  (POI / Accommodation path) ──────────────────────────────────
    public static final String POI_NAME           = "Name of Hotel or Teahouse or Viewpoint or HealthP";

    // ── LEVEL 2  Hotel / Tea House ────────────────────────────────────────────
    public static final String ADDRESS            = "Address";
    public static final String OWNER_NAME         = "Owner Name/Person Name";
    public static final String CONTACT            = "Contact Information";
    public static final String HAS_ROOMS          = "Do they have any rooms?";
    public static final String HOT_WATER          = "Availability of hot water?";
    public static final String HOT_WATER_CHARGE   = "Do you charge guests separately for hot water?";
    public static final String TOTAL_TOILETS      = "Total Number of Toilets";
    public static final String ATTACHED_BATHROOMS = "Number of Attached Bathrooms";
    public static final String COMMON_TOILETS     = "Number of Common Toilets";
    public static final String NUM_STAFF          = "Number of Staff";
    public static final String PHOTO              = "Photo: Board, Room, Menu, Toilet, Beds, Hotel or Tree House as a Whole and Individual, Dinning Room, Location and etc?";
    public static final String ELECTRICITY_PARENT = "Main Source of Electricity";
    public static final String WATER_PARENT       = "Main Source of Drinking Water";
    public static final String FIRST_AID          = "First Aid Kit Available";

    // ── LEVEL 2b  Has Rooms == "Yes" ──────────────────────────────────────────
    public static final String TOTAL_ROOMS        = "Total Number of Rooms";
    public static final String SINGLE_ROOMS       = "Number of Single Rooms";
    public static final String DOUBLE_ROOMS       = "Number of Double Rooms";
    public static final String GROUP_ROOM         = "Group Room (Bed/Room)";
    public static final String MAX_CAPACITY       = "Maximum Guest Capacity at Full Occupancy";
    public static final String PRICE_SINGLE_NP    = "Price per single room (Nepali)?";
    public static final String PRICE_PKG_NP       = "Price per package (Nepali)? Breakfast, Dinner, and Room";
    public static final String PRICE_SINGLE_FG    = "Price per single room (Foreigner)?";
    public static final String PRICE_PKG_FG       = "Price per package (Foreigner)? Breakfast, Dinner, and Room";

    // ── LEVEL 3  Electricity sub-cols ─────────────────────────────────────────
    public static final String ELEC_GRID          = "Main Source of Electricity/Grid";
    public static final String ELEC_SOLAR         = "Main Source of Electricity/Solar";
    public static final String ELEC_GENERATOR     = "Main Source of Electricity/Generator";
    public static final String ELEC_MIXED         = "Main Source of Electricity/Mixed";
    public static final String ELEC_NONE          = "Main Source of Electricity/No Electricity";

    // ── LEVEL 3  Water sub-cols ───────────────────────────────────────────────
    public static final String WATER_SPRING       = "Main Source of Drinking Water/Water Spring";
    public static final String WATER_BOREWELL     = "Main Source of Drinking Water/Borewell";
    public static final String WATER_RIVER        = "Main Source of Drinking Water/River";
    public static final String WATER_MUNICIPAL    = "Main Source of Drinking Water/Municipal";
    public static final String WATER_MIXED        = "Main Source of Drinking Water/Mixed";

    // ── LEVEL 3  First-aid detail ─────────────────────────────────────────────
    public static final String FIRST_AID_KIND     = "What Kind of First Aid Kit is Available?";

    // ── LEVEL 2  Hospital ─────────────────────────────────────────────────────
    public static final String HEALTH_POST_DETAIL = "Detail Information of Health Post? What kind of medicine do they have? How many beds are there? ";

    // ── LEVEL 2  Water trail / drinking-water stop ────────────────────────────
    public static final String WATER_SOURCE_KIND  = "What Kind of Water Source?";
    public static final String WATER_SOURCE_NAME  = "Name of Water Source";
    public static final String PHOTO_NUMBER       = "Photo Number";

    // ── Required columns – checked before the first row is processed ──────────
    public static final List<String> REQUIRED_COLUMNS =
            List.of(WAYPOINT_NUMBER, TRAIL_PATH, IMPORTANT_STOPS);
}
