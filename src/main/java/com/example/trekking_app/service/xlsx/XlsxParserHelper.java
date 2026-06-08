package com.example.trekking_app.service.xlsx;

import com.example.trekking_app.dto.trackpoint.TrackPointInfo;
import com.example.trekking_app.entity.*;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import com.example.trekking_app.mapper.TrackPointMapper;
import com.example.trekking_app.model.*;
import com.example.trekking_app.repository.GpxSegmentRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import com.example.trekking_app.repository.WayPointRepository;
import com.example.trekking_app.service.gpx.GpxMergeHelper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

import static com.example.trekking_app.service.xlsx.XlsxParserColumnDependency.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class XlsxParserHelper {
    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private final WayPointRepository wayPointRepo;
    private final GpxSegmentRepository gpxSegmentRepo;
    private final TrackPointRepository trackPointRepo;
    private final GpxMergeHelper gpxMergeHelper;
    private final TrackPointMapper tpMapper = new TrackPointMapper();


    private static final Set<String> ACCOMMODATION_STOPS = Set.of("hotel", "tea house");
    private static final String STOP_HOSPITAL = "hospital";
    private static final String STOP_DRINKING_WATER = "drinking water";
    private static final String STOP_RESTROOM = "restroom";
    private static final String STOP_REST_AREA = "rest area";
    private static final String STOP_TRASH = "trash can";
    private static final String STOP_VIEW_POINT = "view point";

    // ── Routing tokens ────────────────────────────────────────────────────────
    private static final String PATH_OTHER = "other";
    private static final String START_TOKEN = "start";
    private static final String END_TOKEN = "end";
    private static final String YES = "yes";


    public XlsxParserResult parseRow(Row row, Map<String, Integer> indexMap, Route route, Integer gpxOrderIndex,
                                     Map<String,GpxSegment> gpxSegmentCache ,
                                     Map<String,WayPoint> wayPointCache)
    {
            String wpNum = str(row, indexMap, WAYPOINT_NUMBER);
            if (wpNum == null) return null;

        String segmentCacheKey = route.getId()+":"+gpxOrderIndex;
        GpxSegment gpxSegment = null;

            if(!gpxSegmentCache.containsKey(segmentCacheKey)) {
                gpxSegment = gpxSegmentRepo.findByRoute_IdAndSegmentStatusAndOrderIndex(route.getId(), GpxSegmentStatus.WAYPOINT, gpxOrderIndex).orElseThrow(
                        () -> new ResourceNotFoundException("gpx segment", "route id and order index", String.format("%d and %d respectively", route.getId(), gpxOrderIndex))
                );
            }
            else gpxSegment = gpxSegmentCache.get(segmentCacheKey);

            String wayPointCacheKey = route.getId()+":"+gpxSegment.getId()+":"+wpNum;
            WayPoint wayPoint = null;
            int gpxSegmentId = gpxSegment.getId();
            if(!wayPointCache.containsKey(wayPointCacheKey))
                wayPoint = wayPointRepo.findByRoute_IdAndGpxSegment_IdAndLocalSequence(route.getId(), gpxSegment.getId(),Integer.parseInt(wpNum)).orElseThrow(
                    () -> new ResourceNotFoundException("waypoint", "route id , gpx segment id and waypoint name", String.format("%d , %d and %s respectively. failed at row %d", route.getId(), gpxSegmentId,wpNum,row.getRowNum()))
            );
            else wayPoint = wayPointCache.get(wayPointCacheKey);

            try{

            String trailPath = str(row, indexMap, TRAIL_PATH);
            String startOrEnd = str(row, indexMap, START_OR_END);
            String importantStops = str(row, indexMap, IMPORTANT_STOPS);
            String other = str(row, indexMap, OTHER);

            boolean isTrailPath = trailPath != null && !trailPath.trim().equalsIgnoreCase(PATH_OTHER);

            if (isTrailPath && startOrEnd != null) {
                TrailSegment trailSegment = TrailSegment.builder()
                        .route(route)
                        .type(resolveTrailType(trailPath))
                        .gpxSegment(gpxSegment)
                        .build();

                if (START_TOKEN.equalsIgnoreCase(startOrEnd.trim())) trailSegment.setStartWaypoint(wayPoint);
                else
                    trailSegment.setEndWaypoint(wayPoint);

                return XlsxParserResult.ofTrailSegment(wayPoint, trailSegment, gpxOrderIndex);
            }
            String stopKey = importantStops != null ? importantStops.trim().toLowerCase() : null;

            if (ACCOMMODATION_STOPS.contains(stopKey)) {
                String poiName = resolvePoiName(row, indexMap, importantStops, other);
                Accommodation acc = buildAccommodation(row, indexMap, poiName, stopKey, wayPoint, route);
                return XlsxParserResult.ofAccommodation(wayPoint, acc, gpxOrderIndex);
            }

            //Plain POI
            String poiName = resolvePoiName(row, indexMap, importantStops, other);
            if (poiName == null && stopKey == null) return null;

            POI poi = dispatchPlainPOI(row, indexMap, poiName, stopKey, trailPath, wayPoint, route);
            return XlsxParserResult.ofPOI(wayPoint, poi, gpxOrderIndex);
        }
        catch (Exception e)
        {
            log.error("exception thrown from method XlsxParserHelper.parseRow at row {} : {}",row.getRowNum(),e.getLocalizedMessage());
            throw new FileParsingFailedException("failed to parse xlsx file");
        }
    }

    public ParseOutput partition(List<XlsxParserResult> rawRows , Route route)
    {
        List<WayPoint>      wayPoints      = new ArrayList<>();
        List<POI>           pois           = new ArrayList<>();
        List<Accommodation> accommodations = new ArrayList<>();
        List<TrailSegment>  trailSegments       = new ArrayList<>();
        Map<String,TrailSegment> pendingStarts = new LinkedHashMap<>();

        //store completed trail segments for batch processing
        List<TrailSegment> completedTrailSegments = new ArrayList<>();

        //store trackpoint id mappings for later use
        Map<TrackPoint,Integer> segmentStartTpIds = new HashMap<>();
        Map<TrackPoint,Integer> segmentEndTpsIds = new HashMap<>();

        for(XlsxParserResult r : rawRows)
        {
            wayPoints.add(r.getWayPoint());
            switch(r.getOutcomeType())
            {
                case TRAIL_SEGMENT ->
                {
                    TrailSegment stub = r.getTrailSegment();
                    String pairingKey = r.getGpxOrderIndex() + ":" + stub.getType().name();
                    if(stub.getStartWaypoint() != null)
                        pendingStarts.put(pairingKey,stub);
                    else if(stub.getEndWaypoint() != null)
                    {
                        TrailSegment startStub = pendingStarts.remove(pairingKey);
                        if(startStub != null)
                        {
                            startStub.setEndWaypoint(stub.getEndWaypoint());
                            completedTrailSegments.add(startStub);
                            /*
                            LineString path = generateTrailSegmentPath(startStub.getStartWaypoint(),startStub.getEndWaypoint(),route);
                            startStub.setPath(path);
                            trailSegments.add(startStub);

                             */
                        }
                    }
                }
                case ACCOMMODATION ->  accommodations.add(r.getAccommodation());
                case POI -> pois.add(r.getPoi());
            }
        }
        if(!completedTrailSegments.isEmpty())
            trailSegments.addAll(generateAllTrailSegmentPaths(completedTrailSegments,route));

        return new ParseOutput(rawRows,wayPoints,pois,accommodations,trailSegments);
    }

   @Transactional
    public List<TrailSegment> generateAllTrailSegmentPaths(List<TrailSegment> segments,Route route)
    {
          if(segments.isEmpty())
          {
              log.warn("no trail segments to process");
              return null;
          }
          log.info("Starting batch path generation for {} segments",segments.size());

          Map<TrailSegment, TrackPointInfo> startTpInfos = new HashMap<>();
          Map<TrailSegment,TrackPointInfo>  endTpInfos = new HashMap<>();

          segments.forEach(segment -> {
              TrackPoint startTp = resolveOrInsertTrackPoint(segment.getStartWaypoint(),route);
              TrackPoint endTp = resolveOrInsertTrackPoint(segment.getEndWaypoint(),route);

              if(startTp != null) startTpInfos.put(segment, tpMapper.toTrackPointInfo(startTp));

              if (endTp != null) endTpInfos.put(segment,tpMapper.toTrackPointInfo(endTp));

          });
          log.info("Recalculating global sequences for route {}",route.getId());
          gpxMergeHelper.assignTrackPointGlobalSequences(route.getId());

          log.info("Generating Line String for trail segments {}",Instant.now());
          for(TrailSegment segment : segments)
          {
                  TrackPointInfo startTpInfo = startTpInfos.get(segment);
                  TrackPointInfo endTpInfo = endTpInfos.get(segment);

                  if (startTpInfo == null || endTpInfo == null) {
                      log.warn("Missing trackpoint info for segment {}, using straight line", segment.getId());
                      segment.setPath(straightLine(segment.getStartWaypoint(), segment.getEndWaypoint()));
                      continue;
                  }
                  Optional<TrackPoint> startTp = trackPointRepo.findById(startTpInfo.getId());
                  Optional<TrackPoint> endTp = trackPointRepo.findById(endTpInfo.getId());

                  if (startTp.isEmpty() || endTp.isEmpty()) {
                      log.warn("Missing trackpoint from database for segment {}, using straight line", segment.getId());
                      segment.setPath(straightLine(segment.getStartWaypoint(), segment.getEndWaypoint()));
                      continue;
                  }
                  LineString path = generateTrailSegmentPath(startTp.get(), endTp.get(), route);
                  segment.setPath(path);

          }
          log.info("Finished generating line string at {}", Instant.now());
          return segments;

    }
    @Transactional
    private LineString generateTrailSegmentPath(@NonNull TrackPoint startTp, @NonNull TrackPoint endTp,@NonNull Route route)
    {
        Integer startSeq = startTp.getGlobalSequence();
        Integer endSeq = endTp.getGlobalSequence();

        if (startSeq == null || endSeq == null) {
            log.debug("Global sequences not yet assigned for TrackPoint ids {} and {}, using straight line",
                    startTp.getId(), endTp.getId());
            return straightLine(startTp,endTp);
        }
       if(startSeq>endSeq) {Integer temp = startSeq;startSeq = endSeq; endSeq = startSeq;}
        List<TrackPoint> points = trackPointRepo.findBetweenGlobalSequences(route.getId(), startSeq, endSeq);
        if (points.size() < 2)
        {
            log.warn("Not enough trackpoints ({}) between globalSeq {} and {} for route {}", points.size(), startSeq, endSeq, route.getId());
            return straightLine(startTp, endTp);
        }

        Coordinate[] coords = points.stream()
                .map(tp -> new Coordinate(tp.getLongitude(), tp.getLatitude()))
                .toArray(Coordinate[]::new);
        return GF.createLineString(coords);

    }


 @Deprecated(since = "version 2.0")
    public LineString generateTrailSegmentPath(WayPoint start, WayPoint end, Route route) {

        TrackPoint startTp = resolveOrInsertTrackPoint(start, route);
        TrackPoint endTp   = resolveOrInsertTrackPoint(end, route);

        if (startTp == null || endTp == null) {
            log.warn("Could not resolve trackpoints for start/end waypoints on route {}, falling back to straight line", route.getId());
            return straightLine(start, end);
        }
        //update global sequence
        gpxMergeHelper.assignTrackPointGlobalSequences(route.getId());

        Integer startSeq = trackPointRepo.findById(startTp.getId()).map(TrackPoint::getGlobalSequence).orElseThrow(
                () -> new FileParsingFailedException("failed to create trail segment between wp :"+start+"and"+end)
        );

        Integer endSeq   = trackPointRepo.findById(endTp.getId()).map(TrackPoint::getGlobalSequence).orElseThrow(
                () -> new FileParsingFailedException("failed to create trail segment between wp :"+start+"and"+end)
        );

        if (startSeq > endSeq) { Integer tmp = startSeq; startSeq = endSeq; endSeq = tmp; }

        List<TrackPoint> points = trackPointRepo.findBetweenGlobalSequences(route.getId(), startSeq, endSeq);
        if (points.size() < 2) {
            log.warn("Not enough trackpoints ({}) between globalSeq {} and {} for route {}",
                    points.size(), startSeq, endSeq, route.getId());
            return straightLine(start, end);
        }

        Coordinate[] coords = points.stream()
                .map(tp -> new Coordinate(tp.getLongitude(), tp.getLatitude()))
                .toArray(Coordinate[]::new);
        return GF.createLineString(coords);
    }

    /**
     * Returns existing TrackPoint if coordinates already persisted,
     * otherwise finds predecessor, inserts new TP, shifts sequences.
     */
    @Transactional
    private TrackPoint resolveOrInsertTrackPoint(WayPoint wayPoint, Route route) {

        // 1. Check if already exists
        Optional<TrackPoint> existing = trackPointRepo
                .findFirstByLatitudeAndLongitude(wayPoint.getLatitude(), wayPoint.getLongitude());
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. Find the trackpoint just before this waypoint's position
        TrackPoint predecessor = trackPointRepo
                .findPredecessorByCoordinates(route.getId(), wayPoint.getLatitude(), wayPoint.getLongitude())
                .orElse(null);

        if (predecessor == null) {
            log.warn("No predecessor trackpoint found for waypoint [{},{}] on route {}",
                    wayPoint.getLatitude(), wayPoint.getLongitude(), route.getId());
            return null;
        }

        // 3. Shift localSequence of all TPs in same segment after predecessor
        trackPointRepo.shiftLocalSequencesAfter(
                predecessor.getGpxSegment().getId(),
                predecessor.getLocalSequence()
        );

        // 4. Insert new trackpoint
        TrackPoint newTp = TrackPoint.builder()
                .latitude(wayPoint.getLatitude())
                .longitude(wayPoint.getLongitude())
                .localSequence(predecessor.getLocalSequence() + 1)
                .gpxSegment(predecessor.getGpxSegment())
                .route(route)
                .build();

        TrackPoint saved = trackPointRepo.save(newTp);
        log.info("Inserted new TrackPoint id={} at localSequence={} in segment={}",
                saved.getId(), saved.getLocalSequence(), saved.getGpxSegment().getId());

        return saved;
    }

    private LineString straightLine(WayPoint start, WayPoint end) {
        return GF.createLineString(new Coordinate[]{
                new Coordinate(start.getLongitude(), start.getLatitude()),
                new Coordinate(end.getLongitude(), end.getLatitude())
        });
    }
    private LineString straightLine(TrackPoint start, TrackPoint end) {
        return GF.createLineString(new Coordinate[]{
                new Coordinate(start.getLongitude(), start.getLatitude()),
                new Coordinate(end.getLongitude(), end.getLatitude())
        });
    }
    public void validateRequiredColumns(Map<String, Integer> idx) {
        List<String> missing = REQUIRED_COLUMNS.stream()
                .filter(col -> !idx.containsKey(col))
                .toList();
        if (!missing.isEmpty())
            throw new IllegalArgumentException(
                    "XLSX is missing required columns: " + missing
                            + "\nHeaders found: " + idx.keySet());
    }

    public Map<String, Integer> buildColumnIndex(Row columns) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (columns == null) return map;
        for (Cell c : columns) {
            String h = rawStr(c);
            if (h != null && !h.isBlank())
                map.put(h.trim(), c.getColumnIndex());
        }
        return map;
    }

    public String rawStr(Cell c) {
        if (c == null) return null;
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue();
            case NUMERIC -> (c.getNumericCellValue() == Math.floor(c.getNumericCellValue()))
                    ? String.valueOf((long) c.getNumericCellValue())
                    : String.valueOf(c.getNumericCellValue());
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(c.getNumericCellValue());
                } catch (Exception e) {
                    yield c.getStringCellValue();
                }
            }
            default -> null;
        };
    }

    public Double dbl(Row row, Map<String, Integer> idx, String col) {
        Integer i = idx.get(col);
        if (i == null) return null;
        Cell c = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return null;
        try {
            return switch (c.getCellType()) {
                case NUMERIC -> c.getNumericCellValue();
                case STRING -> {
                    String s = c.getStringCellValue().trim();
                    yield s.isEmpty() ? null : Double.parseDouble(s);
                }
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean isBlank(Row row) {
        for (Cell c : row)
            if (c != null && c.getCellType() != CellType.BLANK) return false;
        return true;
    }

    public String str(Row row, Map<String, Integer> idx, String col) {
        Integer i = idx.get(col);
        if (i == null) return null;
        Cell c = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        String v = rawStr(c);
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    public TrailType resolveTrailType(String trailPath) {
        if (trailPath == null) return TrailType.OTHER;
        return switch (trailPath.toLowerCase().trim()) {
            case "water" -> TrailType.WATER;
            case "rocky trail" -> TrailType.ROCKY_TRAIL;
            case "stone stairs trail" -> TrailType.STONE_STAIRS;
            case "earthen forest trail" -> TrailType.EARTHEN_FOREST_TRAIL;
            case "concrete stairs" -> TrailType.CONCRETE_STAIRS;
            case "dhunga chapeko" -> TrailType.DHUNGA_CHAPEKO;
            default -> TrailType.OTHER;
        };
    }
    private String resolvePoiName(Row row, Map<String, Integer> idx,
                                  String importantStops, String other) {
        String fromCol = str(row, idx, POI_NAME);
        if (fromCol != null) return fromCol;
        if (other    != null) return other;
        return importantStops;
    }

    private String derivePoiDisplayName(String importantStops, String other, String number) {
        if (other          != null && !other.isBlank())          return other.trim();
        if (importantStops != null && !importantStops.isBlank()) return importantStops.trim();
        return "Waypoint " + number;
    }

    private String buildWaterDesc(String kind, String name) {
        if (kind == null && name == null) return null;
        StringBuilder sb = new StringBuilder();
        if (kind != null) sb.append("Type: ").append(kind);
        if (name != null) {
            if (!sb.isEmpty()) sb.append(" | ");
            sb.append("Name: ").append(name);
        }
        return sb.toString();
    }

    private POI dispatchPlainPOI(Row row, Map<String, Integer> idx,
                                 String poiName, String stopKey,
                                 String trailPath, WayPoint wayPoint, Route route) {

        if (STOP_HOSPITAL.equals(stopKey)) {
            // LEVEL 2c: health post detail — only for Hospital stop
            String detail = str(row, idx, HEALTH_POST_DETAIL);
            return buildPOI(
                    poiName != null ? poiName : "Health Post",
                    POIType.HEALTH_POST, detail, wayPoint, route);
        }

        if (STOP_DRINKING_WATER.equals(stopKey)
                || "water".equalsIgnoreCase(trailPath)) {
            // LEVEL 2d: water source detail — Drinking Water stop OR Water trail path
            String kind = str(row, idx, WATER_SOURCE_KIND);
            String name = str(row, idx, WATER_SOURCE_NAME);
            return buildPOI(
                    poiName != null ? poiName : "Drinking Water",
                    POIType.WATER_SOURCE,
                    buildWaterDesc(kind, name),
                    wayPoint, route);
        }

        // All remaining: Restroom / Rest Area / Trash Can / View Point / Other
        // No extra columns read — type inferred from stopKey alone
        POIType type  = resolveSimplePOIType(stopKey);
        String  label = poiName != null ? poiName
                : (stopKey != null ? capitalize(stopKey)
                   : "Waypoint " + wayPoint.getName());
        return buildPOI(label, type, null, wayPoint, route);
    }

    private POI buildPOI(String name, POIType type, String description,
                         WayPoint wayPoint, Route route) {
        return POI.builder()
                .route(route)
                .wayPoint(wayPoint)
                .name(name)
                .type(type)
                .description(description)
                .latitude(wayPoint.getLatitude())
                .longitude(wayPoint.getLongitude())
                .elevation(wayPoint.getElevation() != null ? wayPoint.getElevation() : 0.0)
                .build();
    }
    private POIType resolveSimplePOIType(String stopKey) {
        if (stopKey == null) return POIType.OTHER;
        return switch (stopKey) {
            case STOP_RESTROOM       -> POIType.RESTROOM;
            case STOP_REST_AREA      -> POIType.REST_AREA;
            case STOP_TRASH          -> POIType.TRASH_CAN;
            case STOP_VIEW_POINT     -> POIType.VIEWPOINT;
            case STOP_DRINKING_WATER -> POIType.WATER_SOURCE;
            default                  -> POIType.OTHER;
        };
    }
    public Accommodation buildAccommodation(Row row,Map<String,Integer> idx,String poiName,String stopKey,
                                           WayPoint wayPoint , Route route)
    {
        // ── LEVEL 2: always read for accommodation rows ───────────────────────
        String  address     = str(row,    idx, ADDRESS);
        String  ownerName   = str(row,    idx, OWNER_NAME);   // see extended fields below
        String  contact     = str(row,    idx, CONTACT);
        String  hasRoomsStr = str(row,    idx, HAS_ROOMS);
        Integer numStaff    = intVal(row, idx, NUM_STAFF);

        // ── LEVEL 3: electricity sub-cols (only when parent col != null) ──────
        ElectricitySource electricitySource = null;
        if (str(row, idx, ELECTRICITY_PARENT) != null)
            electricitySource = resolveElectricitySource(row, idx);

        // ── LEVEL 3: water sub-cols (only when parent col != null) ────────────
        WaterSource waterSource = null;
        if (str(row, idx, WATER_PARENT) != null)
            waterSource = resolveWaterSource(row, idx);

        // ── LEVEL 3: first-aid detail (only when First Aid == "Yes") ─────────
        Boolean hasFirstAid  = parseYesNo(str(row, idx, FIRST_AID));
        String  firstAidKind = Boolean.TRUE.equals(hasFirstAid)
                ? str(row, idx, FIRST_AID_KIND) : null;  // guarded read

        // ── LEVEL 2b: room & price data (only when Has Rooms == "Yes") ────────
        boolean hasRooms      = YES.equalsIgnoreCase(hasRoomsStr);
        Integer totalRooms    = null;
        Integer singleRooms   = null;
        Integer doubleRooms   = null;
        String  groupRoom     = null;
        Integer maxCapacity   = null;
        Double  priceSingleNp = null;
        Double  pricePkgNp    = null;
        Double  priceSingleFg = null;
        Double  pricePkgFg    = null;

        if (hasRooms) {
            totalRooms    = intVal(row, idx, TOTAL_ROOMS);
            singleRooms   = intVal(row, idx, SINGLE_ROOMS);
            doubleRooms   = intVal(row, idx, DOUBLE_ROOMS);
            groupRoom     = str(row,   idx, GROUP_ROOM);
            maxCapacity   = intVal(row, idx, MAX_CAPACITY);
            priceSingleNp = dbl(row,   idx, PRICE_SINGLE_NP);
            pricePkgNp    = dbl(row,   idx, PRICE_PKG_NP);
            priceSingleFg = dbl(row,   idx, PRICE_SINGLE_FG);
            pricePkgFg    = dbl(row,   idx, PRICE_PKG_FG);
        }

        POIType type = "tea house".equals(stopKey)
                ? POIType.TEA_HOUSE : POIType.ACCOMMODATION;

        Accommodation acc = new Accommodation();
                acc.setRoute(route);
                acc.setWayPoint(wayPoint);
                acc.setName(poiName);
                acc.setType(type);
                acc.setElevation(wayPoint.getElevation());
                acc.setLongitude(wayPoint.getLongitude());
                acc.setLatitude(wayPoint.getLatitude());
        acc.setAddress(address);
        acc.setContactNumber(contact);
        acc.setTotalRooms(totalRooms);
        acc.setPriceNepali(priceSingleNp);
        acc.setPriceForeigner(priceSingleFg);
        acc.setElectricitySource(electricitySource);
        acc.setWaterSource(waterSource);
        acc.setHasFirstAid(hasFirstAid);

        return acc;
    }
    /**
     * Reads all five electricity boolean sub-columns.
     * Called ONLY when ELECTRICITY_PARENT != null (LEVEL 3 guard).
     * MIXED wins when more than one source flag is true (e.g. Grid + Solar).
     */
    private ElectricitySource resolveElectricitySource(Row row, Map<String, Integer> idx) {
        boolean grid      = flag(row, idx, ELEC_GRID);
        boolean solar     = flag(row, idx, ELEC_SOLAR);
        boolean generator = flag(row, idx, ELEC_GENERATOR);
        boolean mixed     = flag(row, idx, ELEC_MIXED);
        boolean none      = flag(row, idx, ELEC_NONE);

        if (none)  return ElectricitySource.NONE;
        int count = (grid ? 1 : 0) + (solar ? 1 : 0) + (generator ? 1 : 0);
        if (mixed || count > 1) return ElectricitySource.MIXED;
        if (grid)      return ElectricitySource.GRID;
        if (solar)     return ElectricitySource.SOLAR;
        if (generator) return ElectricitySource.GENERATOR;
        return null;
    }

    /**
     * Reads all five water boolean sub-columns.
     * Called ONLY when WATER_PARENT != null (LEVEL 3 guard).
     */
    private WaterSource resolveWaterSource(Row row, Map<String, Integer> idx) {
        boolean spring    = flag(row, idx, WATER_SPRING);
        boolean borewell  = flag(row, idx, WATER_BOREWELL);
        boolean river     = flag(row, idx, WATER_RIVER);
        boolean municipal = flag(row, idx, WATER_MUNICIPAL);
        boolean mixed     = flag(row, idx, WATER_MIXED);

        int count = (spring ? 1 : 0) + (borewell ? 1 : 0)
                + (river  ? 1 : 0) + (municipal ? 1 : 0);
        if (mixed || count > 1) return WaterSource.MIXED;
        if (spring)    return WaterSource.WATER_SPRING;
        if (borewell)  return WaterSource.BOREWELL;
        if (river)     return WaterSource.RIVER;
        if (municipal) return WaterSource.MUNICIPAL;
        return null;
    }
    /**
     * Boolean sub-column (0/1 numeric or yes/no string).
     * Returns true ONLY on explicit 1 / true / yes.
     * Absent column or null cell → false (safe default).
     */
    private boolean flag(Row row, Map<String, Integer> idx, String col) {
        Integer i = idx.get(col);
        if (i == null) return false;
        Cell c = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return false;
        return switch (c.getCellType()) {
            case NUMERIC -> c.getNumericCellValue() == 1.0;
            case BOOLEAN -> c.getBooleanCellValue();
            case STRING  -> {
                String s = c.getStringCellValue().trim();
                yield "1".equals(s) || YES.equalsIgnoreCase(s);
            }
            default -> false;
        };
    }
    private Boolean parseYesNo(String s) {
        if (s == null) return null;
        return YES.equalsIgnoreCase(s.trim());
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    private Integer intVal(Row row, Map<String, Integer> idx, String col) {
        Double d = dbl(row, idx, col);
        return d == null ? null : d.intValue();
    }

}
