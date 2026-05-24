package com.example.trekking_app.service;

import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.WayPoint;
import com.example.trekking_app.model.ElectricitySource;
import com.example.trekking_app.model.POIType;
import com.example.trekking_app.model.WaterSource;
import com.example.trekking_app.model.WayPointStatus;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.*;

/**
* Parses the Balmiki Ashram trail-map XLSX (sheet "Trail Map") and maps
* each row into WayPoint + POI (or Accommodation subtype) entities.
*
* Dependencies (Maven / Gradle):
*   org.apache.poi : poi-ooxml : 5.x
    */
    public class BalmikiAshramXlsxParser {

// ── Column header constants (exact strings in the XLSX) ──────────────────

private static final String COL_WAYPOINT_NUMBER   = "Waypoint Number";
private static final String COL_TRAIL_PATH        = "Trail Path";
private static final String COL_START_OR_END      = "Start or End";
private static final String COL_IMPORTANT_STOPS   = "Important Stops";
private static final String COL_OTHER             = "Other";

private static final String COL_POI_NAME          = "Name of Hotel or Teahouse or Viewpoint or HealthP";
private static final String COL_ADDRESS           = "Address";
private static final String COL_CONTACT           = "Contact Information";

private static final String COL_TOTAL_ROOMS       = "Total Number of Rooms";
private static final String COL_PRICE_NP          = "Price per single room (Nepali)?";
private static final String COL_PRICE_FG          = "Price per single room (Foreigner)?";

private static final String COL_ELEC_GRID         = "Main Source of Electricity/Grid";
private static final String COL_ELEC_SOLAR        = "Main Source of Electricity/Solar";
private static final String COL_ELEC_GENERATOR    = "Main Source of Electricity/Generator";
private static final String COL_ELEC_MIXED        = "Main Source of Electricity/Mixed";
private static final String COL_ELEC_NONE         = "Main Source of Electricity/No Electricity";

private static final String COL_WATER_SPRING      = "Main Source of Drinking Water/Water Spring";
private static final String COL_WATER_BOREWELL    = "Main Source of Drinking Water/Borewell";
private static final String COL_WATER_RIVER       = "Main Source of Drinking Water/River";
private static final String COL_WATER_MUNICIPAL   = "Main Source of Drinking Water/Municipal";
private static final String COL_WATER_MIXED       = "Main Source of Drinking Water/Mixed";

private static final String COL_FIRST_AID         = "First Aid Kit Available";

private static final String SHEET_NAME            = "Trail Map";

// ─────────────────────────────────────────────────────────────────────────

/**
* Parse result container: one WayPoint paired with its optional POI.
* When the row has accommodation data the poi field is an Accommodation
* instance (which extends POI).
  */
  public record ParsedRow(WayPoint wayPoint, POI poi) {}

// ─────────────────────────────────────────────────────────────────────────

/**
* Main parsing entry point.
*
* @param xlsxStream InputStream of Balmiki_Ashram.xlsx
* @param route      the Route entity to associate every record with
* @return ordered list of ParsedRow (one per data row, header row excluded)
  */
  public List<ParsedRow> parse(InputStream xlsxStream, Route route) throws Exception {

  List<ParsedRow> results = new ArrayList<>();

  try (Workbook workbook = new XSSFWorkbook(xlsxStream)) {

       Sheet sheet = workbook.getSheet(SHEET_NAME);
       if (sheet == null) {
           throw new IllegalArgumentException("Sheet '" + SHEET_NAME + "' not found in workbook.");
       }

       // ── Build column-name → index map from the header row ────────────
       Row headerRow = sheet.getRow(0);
       Map<String, Integer> colIndex = buildColumnIndex(headerRow);

       // ── Iterate data rows ────────────────────────────────────────────
       for (int i = 1; i <= sheet.getLastRowNum(); i++) {
           Row row = sheet.getRow(i);
           if (row == null || isRowBlank(row)) continue;

           ParsedRow parsed = mapRow(row, colIndex, route, i);
           if (parsed != null) {
               results.add(parsed);
           }
       }
  }

  return results;
  }

// ─────────────────────────────────────────────────────────────────────────
// Row → entity mapping
// ─────────────────────────────────────────────────────────────────────────

private ParsedRow mapRow(Row row, Map<String, Integer> idx, Route route, int rowNum) {

     // ── WayPoint ─────────────────────────────────────────────────────────
     String waypointNumberRaw = getString(row, idx, COL_WAYPOINT_NUMBER);
     if (waypointNumberRaw == null || waypointNumberRaw.isBlank()) return null;

     // Preserve leading zeros if present (field type is length-32 String)
     String waypointNumber = waypointNumberRaw.trim();

     int localSeq;
     try {
         localSeq = (int) Double.parseDouble(waypointNumber);
     } catch (NumberFormatException e) {
         localSeq = rowNum; // fallback
     }

     String trailPath     = getString(row, idx, COL_TRAIL_PATH);
     String startOrEnd    = getString(row, idx, COL_START_OR_END);
     String importantStop = getString(row, idx, COL_IMPORTANT_STOPS);
     String otherName     = getString(row, idx, COL_OTHER);

     // Derive the display name for this waypoint
     String wpName = deriveWaypointName(trailPath, importantStop, otherName, waypointNumber);

     WayPoint wayPoint = WayPoint.builder()
             .route(route)
             .waypointNumber(waypointNumber)
             .name(wpName)
             .localSequence(localSeq)
             .globalSequence(localSeq)     // adjust if multi-segment ordering is needed
             .status(WayPointStatus.ACTIVE)
             .isDeleted(false)
             // latitude / longitude / elevation not present in this XLSX sheet;
             // set defaults or populate later from GPS data
             .latitude(0.0)
             .longitude(0.0)
             .build();

     // ── POI / Accommodation ───────────────────────────────────────────────
     POI poi = mapPoi(row, idx, route, wayPoint, importantStop, otherName);

     return new ParsedRow(wayPoint, poi);
}

// ─────────────────────────────────────────────────────────────────────────

private POI mapPoi(Row row,
Map<String, Integer> idx,
Route route,
WayPoint wayPoint,
String importantStop,
String otherName) {

     String poiName = coalesce(getString(row, idx, COL_POI_NAME), otherName, importantStop);
     if (poiName == null || poiName.isBlank()) return null;

     POIType poiType = resolvePOIType(importantStop, otherName, poiName);

     // Accommodation sub-type: present when "Total Number of Rooms" has a value
     Double totalRooms = getDouble(row, idx, COL_TOTAL_ROOMS);
     boolean isAccommodation = poiType == POIType.ACCOMMODATION
             || (totalRooms != null && totalRooms > 0);

     if (isAccommodation) {
         return buildAccommodation(row, idx, route, wayPoint, poiName, poiType);
     }

     // Plain POI
     return POI.builder()
             .route(route)
             .wayPoint(wayPoint)
             .name(poiName)
             .type(poiType)
             .latitude(wayPoint.getLatitude())
             .longitude(wayPoint.getLongitude())
             .elevation(wayPoint.getElevation() != null ? wayPoint.getElevation() : 0.0)
             .build();
}

// ─────────────────────────────────────────────────────────────────────────

private Accommodation buildAccommodation(Row row,
Map<String, Integer> idx,
Route route,
WayPoint wayPoint,
String poiName,
POIType poiType) {

     String  address     = getString(row, idx, COL_ADDRESS);
     String  contact     = getString(row, idx, COL_CONTACT);
     Double  totalRooms  = getDouble(row,  idx, COL_TOTAL_ROOMS);
     Double  priceNp     = getDouble(row,  idx, COL_PRICE_NP);
     Double  priceFg     = getDouble(row,  idx, COL_PRICE_FG);
     Boolean hasFirstAid = getBoolean(row, idx, COL_FIRST_AID);

     ElectricitySource electricitySource = resolveElectricitySource(row, idx);
     WaterSource       waterSource       = resolveWaterSource(row, idx);

     Accommodation acc = new Accommodation();

     // BaseEntity fields are set by @CreationTimestamp / @UpdateTimestamp

     // POI (parent) fields
     acc.setRoute(route);
     acc.setWayPoint(wayPoint);
     acc.setName(poiName);
     acc.setType(coalesce(poiType, POIType.ACCOMMODATION));
     acc.setLatitude(wayPoint.getLatitude());
     acc.setLongitude(wayPoint.getLongitude());
     acc.setElevation(wayPoint.getElevation() != null ? wayPoint.getElevation() : 0.0);

     // Accommodation-specific fields
     acc.setAddress(address);
     acc.setContactNumber(contact);
     acc.setTotalRooms(totalRooms != null ? totalRooms.intValue() : null);
     acc.setPriceNepali(priceNp);
     acc.setPriceForeigner(priceFg);
     acc.setElectricitySource(electricitySource);
     acc.setWaterSource(waterSource);
     acc.setHasFirstAid(hasFirstAid);

     return acc;
}

// ─────────────────────────────────────────────────────────────────────────
// Enum / type resolution helpers
// ─────────────────────────────────────────────────────────────────────────

/**
* The XLSX encodes electricity source across boolean sub-columns.
* Reads all five and returns the first that is "1" / "true" / "yes".
  */
  private ElectricitySource resolveElectricitySource(Row row, Map<String, Integer> idx) {
  if (isTrue(row, idx, COL_ELEC_GRID))      return ElectricitySource.GRID;
  if (isTrue(row, idx, COL_ELEC_SOLAR))     return ElectricitySource.SOLAR;
  if (isTrue(row, idx, COL_ELEC_GENERATOR)) return ElectricitySource.GENERATOR;
  if (isTrue(row, idx, COL_ELEC_MIXED))     return ElectricitySource.MIXED;
  if (isTrue(row, idx, COL_ELEC_NONE))      return ElectricitySource.NO_ELECTRICITY;
  return null;
  }

/**
* Same pattern for water source sub-columns.
  */
  private WaterSource resolveWaterSource(Row row, Map<String, Integer> idx) {
  if (isTrue(row, idx, COL_WATER_SPRING))   return WaterSource.WATER_SPRING;
  if (isTrue(row, idx, COL_WATER_BOREWELL)) return WaterSource.BOREWELL;
  if (isTrue(row, idx, COL_WATER_RIVER))    return WaterSource.RIVER;
  if (isTrue(row, idx, COL_WATER_MUNICIPAL))return WaterSource.MUNICIPAL;
  if (isTrue(row, idx, COL_WATER_MIXED))    return WaterSource.MIXED;
  return null;
  }

/**
* Infer POIType from the "Important Stops" / "Other" / name columns.
*
* Extend the keyword lists below as new data arrives.
  */
  private POIType resolvePOIType(String importantStop, String other, String name) {
  String lower = name == null ? "" : name.toLowerCase();

  if ("Restroom".equalsIgnoreCase(importantStop)) return POIType.RESTROOM;

  if (lower.contains("hotel") || lower.contains("teahouse")
  || lower.contains("lodge") || lower.contains("guest")) {
  return POIType.ACCOMMODATION;
  }
  if (lower.contains("health") || lower.contains("hospital")
  || lower.contains("clinic")) {
  return POIType.HEALTH_POST;
  }
  if (lower.contains("mandir") || lower.contains("temple")
  || lower.contains("ashram") || lower.contains("chakra")
  || lower.contains("shivalaya")) {
  return POIType.RELIGIOUS_SITE;
  }
  if (lower.contains("well") || lower.contains("water")) {
  return POIType.WATER_SOURCE;
  }
  if (lower.contains("viewpoint") || lower.contains("view")) {
  return POIType.VIEWPOINT;
  }
  return POIType.OTHER;
  }

/**
* Builds a human-readable waypoint name from available columns.
  */
  private String deriveWaypointName(String trailPath,
  String importantStop,
  String other,
  String waypointNumber) {
  String base = coalesce(other, importantStop, trailPath);
  if (base != null && !base.isBlank()) return base.trim();
  return "Waypoint " + waypointNumber;
  }

// ─────────────────────────────────────────────────────────────────────────
// Low-level cell-reading utilities
// ─────────────────────────────────────────────────────────────────────────

private Map<String, Integer> buildColumnIndex(Row headerRow) {
Map<String, Integer> map = new HashMap<>();
if (headerRow == null) return map;
for (Cell cell : headerRow) {
String header = getCellString(cell);
if (header != null && !header.isBlank()) {
map.put(header.trim(), cell.getColumnIndex());
}
}
return map;
}

private String getString(Row row, Map<String, Integer> idx, String colName) {
Integer col = idx.get(colName);
if (col == null) return null;
Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
return getCellString(cell);
}

private Double getDouble(Row row, Map<String, Integer> idx, String colName) {
Integer col = idx.get(colName);
if (col == null) return null;
Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
if (cell == null) return null;
try {
return switch (cell.getCellType()) {
case NUMERIC -> cell.getNumericCellValue();
case STRING  -> {
String s = cell.getStringCellValue().trim();
yield s.isEmpty() ? null : Double.parseDouble(s);
}
default -> null;
};
} catch (NumberFormatException e) {
return null;
}
}

private Boolean getBoolean(Row row, Map<String, Integer> idx, String colName) {
String val = getString(row, idx, colName);
if (val == null) return null;
return val.equalsIgnoreCase("yes") || val.equals("1") || val.equalsIgnoreCase("true");
}

/**
* Returns true if the boolean sub-column cell contains 1 / true / yes.
  */
  private boolean isTrue(Row row, Map<String, Integer> idx, String colName) {
  Boolean b = getBoolean(row, idx, colName);
  return Boolean.TRUE.equals(b);
  }

private String getCellString(Cell cell) {
if (cell == null) return null;
return switch (cell.getCellType()) {
case STRING  -> cell.getStringCellValue().trim();
case NUMERIC -> {
double d = cell.getNumericCellValue();
// Return integer string for whole numbers (e.g. waypoint "1" not "1.0")
yield (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
}
case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
case FORMULA -> {
try { yield String.valueOf(cell.getNumericCellValue()); }
catch (Exception e) { yield cell.getStringCellValue(); }
}
default -> null;
};
}

private boolean isRowBlank(Row row) {
for (Cell cell : row) {
if (cell != null && cell.getCellType() != CellType.BLANK) return false;
}
return true;
}

@SafeVarargs
private <T> T coalesce(T... values) {
for (T v : values) {
if (v != null) return v;
}
return null;
}
}