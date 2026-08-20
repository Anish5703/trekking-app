package com.example.trekking_app.service.xlsx;

import com.example.trekking_app.entity.*;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import com.example.trekking_app.model.*;
import com.example.trekking_app.repository.GpxSegmentRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import com.example.trekking_app.repository.WayPointRepository;
import com.example.trekking_app.service.gpx.GpxMergeHelper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.LineString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.example.trekking_app.service.xlsx.XlsxParserColumnDependency.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class XlsxParserHelperTest {

    @Mock
    private WayPointRepository wayPointRepo;
    @Mock
    private GpxSegmentRepository gpxSegmentRepo;
    @Mock
    private TrackPointRepository trackPointRepo;
    @Mock
    private GpxMergeHelper gpxMergeHelper;

    @InjectMocks
    private XlsxParserHelper helper;

    private Workbook workbook;
    private Sheet sheet;
    private Map<String, Integer> indexMap;
    private int rowCounter;

    private Route route;
    private GpxSegment gpxSegment;
    private WayPoint wayPoint;

    @BeforeEach
    void setUp() {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("data");
        rowCounter = 0;

        indexMap = new HashMap<>();
        String[] columns = {
                WAYPOINT_NUMBER, TRAIL_PATH, START_OR_END, IMPORTANT_STOPS, OTHER, POI_NAME,
                ADDRESS, OWNER_NAME, CONTACT, HAS_ROOMS, NUM_STAFF,
                ELECTRICITY_PARENT, ELEC_GRID, ELEC_SOLAR, ELEC_GENERATOR, ELEC_MIXED, ELEC_NONE,
                WATER_PARENT, WATER_SPRING, WATER_BOREWELL, WATER_RIVER, WATER_MUNICIPAL, WATER_MIXED,
                FIRST_AID, FIRST_AID_KIND,
                TOTAL_ROOMS, SINGLE_ROOMS, DOUBLE_ROOMS, GROUP_ROOM, MAX_CAPACITY,
                PRICE_SINGLE_NP, PRICE_PKG_NP, PRICE_SINGLE_FG, PRICE_PKG_FG
        };
        for (int i = 0; i < columns.length; i++) {
            indexMap.put(columns[i], i);
        }

        route = new Route();
        route.setId(1);

        gpxSegment = new GpxSegment();
        gpxSegment.setId(5);

        wayPoint = new WayPoint();
        wayPoint.setId(20);
        wayPoint.setLatitude(27.700);
        wayPoint.setLongitude(86.700);
        wayPoint.setElevation(1200.0);
    }

    private Row row(Map<String, Object> values) {
        Row r = sheet.createRow(rowCounter++);
        values.forEach((key, value) -> {
            Integer col = indexMap.get(key);
            Cell cell = r.createCell(col);
            if (value instanceof Number number) cell.setCellValue(number.doubleValue());
            else if (value instanceof Boolean bool) cell.setCellValue(bool);
            else cell.setCellValue(value.toString());
        });
        return r;
    }

    @Test
    void testBuildColumnIndex_NullRow_ReturnsEmptyMap() {
        Map<String, Integer> result = helper.buildColumnIndex(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testBuildColumnIndex_BuildsMapFromHeaders() {
        Sheet headerSheet = workbook.createSheet("header");
        Row headerRow = headerSheet.createRow(0);
        headerRow.createCell(0).setCellValue(WAYPOINT_NUMBER);
        headerRow.createCell(1).setCellValue(TRAIL_PATH);
        headerRow.createCell(2).setCellValue("");

        Map<String, Integer> result = helper.buildColumnIndex(headerRow);

        assertEquals(0, result.get(WAYPOINT_NUMBER));
        assertEquals(1, result.get(TRAIL_PATH));
        assertEquals(2, result.size());
    }

    @Test
    void testValidateRequiredColumns_AllPresent_DoesNotThrow() {
        assertDoesNotThrow(() -> helper.validateRequiredColumns(indexMap));
    }

    @Test
    void testValidateRequiredColumns_MissingColumns_ThrowsIllegalArgumentException() {
        Map<String, Integer> incomplete = new HashMap<>(indexMap);
        incomplete.remove(WAYPOINT_NUMBER);

        assertThrows(IllegalArgumentException.class, () -> helper.validateRequiredColumns(incomplete));
    }

    @Test
    void testStr_ReturnsTrimmedValue_WhenPresent() {
        Row r = row(Map.of(POI_NAME, "  Teahouse A  "));

        assertEquals("Teahouse A", helper.str(r, indexMap, POI_NAME));
    }

    @Test
    void testStr_ReturnsNull_WhenColumnMissingFromIndex() {
        Row r = row(Map.of(POI_NAME, "Teahouse A"));

        assertNull(helper.str(r, indexMap, PHOTO_NUMBER));
    }

    @Test
    void testStr_ReturnsNull_WhenCellBlank() {
        Row r = row(Map.of());

        assertNull(helper.str(r, indexMap, POI_NAME));
    }

    @Test
    void testDbl_ReturnsNumericValue() {
        Row r = row(Map.of(NUM_STAFF, 5));

        assertEquals(5.0, helper.dbl(r, indexMap, NUM_STAFF));
    }

    @Test
    void testDbl_ParsesNumericStringValue() {
        Row r = row(Map.of(WAYPOINT_NUMBER, "7"));

        assertEquals(7.0, helper.dbl(r, indexMap, WAYPOINT_NUMBER));
    }

    @Test
    void testDbl_ReturnsNull_WhenStringNotNumeric() {
        Row r = row(Map.of(WAYPOINT_NUMBER, "abc"));

        assertNull(helper.dbl(r, indexMap, WAYPOINT_NUMBER));
    }

    @Test
    void testDbl_ReturnsNull_WhenColumnMissingFromIndex() {
        Row r = row(Map.of());

        assertNull(helper.dbl(r, indexMap, PHOTO_NUMBER));
    }

    @Test
    void testIsBlank_TrueForEmptyRow() {
        Row r = row(Map.of());

        assertTrue(helper.isBlank(r));
    }

    @Test
    void testIsBlank_FalseWhenAnyCellHasContent() {
        Row r = row(Map.of(POI_NAME, "Teahouse A"));

        assertFalse(helper.isBlank(r));
    }

    @Test
    void testResolveTrailType_KnownValue_ReturnsMappedType() {
        assertEquals(TrailType.BRIDGE, helper.resolveTrailType("Bridge"));
        assertEquals(TrailType.ROCKY_TRAIL, helper.resolveTrailType("rocky trail"));
    }

    @Test
    void testResolveTrailType_UnknownValue_ReturnsOther() {
        assertEquals(TrailType.OTHER, helper.resolveTrailType("quicksand"));
    }

    @Test
    void testResolveTrailType_Null_ReturnsOther() {
        assertEquals(TrailType.OTHER, helper.resolveTrailType(null));
    }

    @Test
    void testBuildAccommodation_HotelWithRooms_PopulatesAllFields() {
        Row r = row(Map.ofEntries(
                Map.entry(ADDRESS, "Main Street"),
                Map.entry(CONTACT, "9800000000"),
                Map.entry(HAS_ROOMS, "Yes"),
                Map.entry(TOTAL_ROOMS, 5),
                Map.entry(SINGLE_ROOMS, 2),
                Map.entry(DOUBLE_ROOMS, 3),
                Map.entry(PRICE_SINGLE_NP, 500),
                Map.entry(PRICE_SINGLE_FG, 10),
                Map.entry(ELECTRICITY_PARENT, "Grid"),
                Map.entry(ELEC_GRID, 1),
                Map.entry(WATER_PARENT, "Spring"),
                Map.entry(WATER_SPRING, 1),
                Map.entry(FIRST_AID, "Yes"),
                Map.entry(FIRST_AID_KIND, "Basic")
        ));

        Accommodation acc = helper.buildAccommodation(r, indexMap, "Hotel A", "hotel", wayPoint, route);

        assertEquals("Hotel A", acc.getName());
        assertEquals(POIType.ACCOMMODATION, acc.getType());
        assertEquals("Main Street", acc.getAddress());
        assertEquals("9800000000", acc.getContactNumber());
        assertEquals(5, acc.getTotalRooms());
        assertEquals(500.0, acc.getPriceNepali());
        assertEquals(10.0, acc.getPriceForeigner());
        assertEquals(ElectricitySource.GRID, acc.getElectricitySource());
        assertEquals(WaterSource.WATER_SPRING, acc.getWaterSource());
        assertTrue(acc.getHasFirstAid());
        assertEquals(wayPoint.getLatitude(), acc.getLatitude());
        assertEquals(wayPoint.getLongitude(), acc.getLongitude());
    }

    @Test
    void testBuildAccommodation_TeaHouse_SetsTypeTeaHouse() {
        Row r = row(Map.of(HAS_ROOMS, "No"));

        Accommodation acc = helper.buildAccommodation(r, indexMap, "Teahouse B", "tea house", wayPoint, route);

        assertEquals(POIType.TEA_HOUSE, acc.getType());
    }

    @Test
    void testBuildAccommodation_NoRooms_LeavesRoomFieldsNull() {
        Row r = row(Map.of(HAS_ROOMS, "No", TOTAL_ROOMS, 5));

        Accommodation acc = helper.buildAccommodation(r, indexMap, "Hotel A", "hotel", wayPoint, route);

        assertNull(acc.getTotalRooms());
    }

    @Test
    void testBuildAccommodation_ElectricityMixed_WhenMultipleFlagsTrue() {
        Row r = row(Map.of(ELECTRICITY_PARENT, "Grid", ELEC_GRID, 1, ELEC_SOLAR, 1));

        Accommodation acc = helper.buildAccommodation(r, indexMap, "Hotel A", "hotel", wayPoint, route);

        assertEquals(ElectricitySource.MIXED, acc.getElectricitySource());
    }

    @Test
    void testBuildAccommodation_WaterSourceSingle_WhenOneFlagTrue() {
        Row r = row(Map.of(WATER_PARENT, "River", WATER_RIVER, 1));

        Accommodation acc = helper.buildAccommodation(r, indexMap, "Hotel A", "hotel", wayPoint, route);

        assertEquals(WaterSource.RIVER, acc.getWaterSource());
    }

    @Test
    void testParseRow_NullWaypointNumber_ReturnsNull() {
        Row r = row(Map.of());

        XlsxParserResult result = helper.parseRow(r, indexMap, route, 1, new HashMap<>(), new HashMap<>());

        assertNull(result);
        verifyNoInteractions(gpxSegmentRepo, wayPointRepo);
    }

    @Test
    void testParseRow_TrailSegmentRow_ReturnsTrailSegmentResult() {
        Row r = row(Map.of(WAYPOINT_NUMBER, 1, TRAIL_PATH, "Bridge", START_OR_END, "Start"));

        when(gpxSegmentRepo.findByRoute_IdAndSegmentStatusAndOrderIndex(route.getId(), GpxSegmentStatus.WAYPOINT, 1))
                .thenReturn(Optional.of(gpxSegment));
        when(wayPointRepo.findByRoute_IdAndGpxSegment_IdAndLocalSequence(route.getId(), gpxSegment.getId(), 1))
                .thenReturn(Optional.of(wayPoint));

        XlsxParserResult result = helper.parseRow(r, indexMap, route, 1, new HashMap<>(), new HashMap<>());

        assertTrue(result.isTrailSegment());
        assertEquals(TrailType.BRIDGE, result.getTrailSegment().getType());
        assertEquals(wayPoint, result.getTrailSegment().getStartWaypoint());
    }

    @Test
    void testParseRow_AccommodationRow_ReturnsAccommodationResult() {
        Row r = row(Map.of(WAYPOINT_NUMBER, 1, IMPORTANT_STOPS, "Hotel", POI_NAME, "Hotel A"));

        when(gpxSegmentRepo.findByRoute_IdAndSegmentStatusAndOrderIndex(route.getId(), GpxSegmentStatus.WAYPOINT, 1))
                .thenReturn(Optional.of(gpxSegment));
        when(wayPointRepo.findByRoute_IdAndGpxSegment_IdAndLocalSequence(route.getId(), gpxSegment.getId(), 1))
                .thenReturn(Optional.of(wayPoint));

        XlsxParserResult result = helper.parseRow(r, indexMap, route, 1, new HashMap<>(), new HashMap<>());

        assertTrue(result.isAccommodation());
        assertEquals("Hotel A", result.getAccommodation().getName());
    }

    @Test
    void testParseRow_PlainPOIRow_ReturnsPOIResult() {
        Row r = row(Map.of(WAYPOINT_NUMBER, 1, IMPORTANT_STOPS, "Restroom"));

        when(gpxSegmentRepo.findByRoute_IdAndSegmentStatusAndOrderIndex(route.getId(), GpxSegmentStatus.WAYPOINT, 1))
                .thenReturn(Optional.of(gpxSegment));
        when(wayPointRepo.findByRoute_IdAndGpxSegment_IdAndLocalSequence(route.getId(), gpxSegment.getId(), 1))
                .thenReturn(Optional.of(wayPoint));

        XlsxParserResult result = helper.parseRow(r, indexMap, route, 1, new HashMap<>(), new HashMap<>());

        assertTrue(result.isPOI());
        assertEquals(POIType.RESTROOM, result.getPoi().getType());
    }

    @Test
    void testParseRow_UsesCache_WhenSegmentAndWaypointAlreadyCached() {
        Row r = row(Map.of(WAYPOINT_NUMBER, 1, IMPORTANT_STOPS, "Restroom"));

        Map<String, GpxSegment> segmentCache = new HashMap<>();
        Map<String, WayPoint> waypointCache = new HashMap<>();
        segmentCache.put(route.getId() + ":1", gpxSegment);
        waypointCache.put(route.getId() + ":" + gpxSegment.getId() + ":1", wayPoint);

        XlsxParserResult result = helper.parseRow(r, indexMap, route, 1, segmentCache, waypointCache);

        assertTrue(result.isPOI());
        verifyNoInteractions(gpxSegmentRepo, wayPointRepo);
    }

    @Test
    void testParseRow_SegmentNotFound_ThrowsResourceNotFoundException() {
        Row r = row(Map.of(WAYPOINT_NUMBER, 1, IMPORTANT_STOPS, "Restroom"));

        when(gpxSegmentRepo.findByRoute_IdAndSegmentStatusAndOrderIndex(route.getId(), GpxSegmentStatus.WAYPOINT, 1))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> helper.parseRow(r, indexMap, route, 1, new HashMap<>(), new HashMap<>()));
    }

    @Test
    void testParseRow_WaypointNotFound_ThrowsResourceNotFoundException() {
        Row r = row(Map.of(WAYPOINT_NUMBER, 1, IMPORTANT_STOPS, "Restroom"));

        when(gpxSegmentRepo.findByRoute_IdAndSegmentStatusAndOrderIndex(route.getId(), GpxSegmentStatus.WAYPOINT, 1))
                .thenReturn(Optional.of(gpxSegment));
        when(wayPointRepo.findByRoute_IdAndGpxSegment_IdAndLocalSequence(route.getId(), gpxSegment.getId(), 1))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> helper.parseRow(r, indexMap, route, 1, new HashMap<>(), new HashMap<>()));
    }

    @Test
    void testPartition_BucketsPOIAndAccommodation() {
        POI poi = new POI();
        Accommodation accommodation = new Accommodation();
        List<XlsxParserResult> rawRows = List.of(
                XlsxParserResult.ofPOI(wayPoint, poi, 1),
                XlsxParserResult.ofAccommodation(wayPoint, accommodation, 1)
        );

        ParseOutput output = helper.partition(rawRows, route);

        assertEquals(1, output.pois().size());
        assertEquals(1, output.accommodations().size());
        assertTrue(output.trailSegments().isEmpty());
        assertEquals(2, output.wayPoints().size());
    }

    @Test
    void testPartition_UnpairedTrailSegmentStart_NotIncludedInTrailSegments() {
        TrailSegment start = new TrailSegment();
        start.setType(TrailType.BRIDGE);
        start.setStartWaypoint(wayPoint);

        List<XlsxParserResult> rawRows = List.of(XlsxParserResult.ofTrailSegment(wayPoint, start, 1));

        ParseOutput output = helper.partition(rawRows, route);

        assertTrue(output.trailSegments().isEmpty());
        verifyNoInteractions(gpxMergeHelper);
    }

    @Test
    void testPartition_PairedTrailSegment_AddedToCompletedList() {
        WayPoint endWayPoint = new WayPoint();
        endWayPoint.setId(21);
        endWayPoint.setLatitude(27.710);
        endWayPoint.setLongitude(86.710);

        TrailSegment start = new TrailSegment();
        start.setType(TrailType.BRIDGE);
        start.setStartWaypoint(wayPoint);

        TrailSegment end = new TrailSegment();
        end.setType(TrailType.BRIDGE);
        end.setEndWaypoint(endWayPoint);

        when(trackPointRepo.findFirstByLatitudeAndLongitude(anyDouble(), anyDouble())).thenReturn(Optional.empty());
        when(trackPointRepo.findPredecessorByCoordinates(eq(route.getId()), anyDouble(), anyDouble())).thenReturn(Optional.empty());

        List<XlsxParserResult> rawRows = List.of(
                XlsxParserResult.ofTrailSegment(wayPoint, start, 1),
                XlsxParserResult.ofTrailSegment(endWayPoint, end, 1)
        );

        ParseOutput output = helper.partition(rawRows, route);

        assertEquals(1, output.trailSegments().size());
        assertNotNull(output.trailSegments().get(0).getPath());
        assertEquals(endWayPoint, output.trailSegments().get(0).getEndWaypoint());
        verify(gpxMergeHelper).assignTrackPointGlobalSequences(route.getId());
    }

    @Test
    void testGenerateAllTrailSegmentPaths_EmptyList_ReturnsNull() {
        List<TrailSegment> result = helper.generateAllTrailSegmentPaths(List.of(), route);

        assertNull(result);
        verifyNoInteractions(gpxMergeHelper, trackPointRepo);
    }

    @Test
    void testGenerateAllTrailSegmentPaths_FallsBackToStraightLine_WhenNoTrackPointsResolved() {
        WayPoint endWayPoint = new WayPoint();
        endWayPoint.setId(21);
        endWayPoint.setLatitude(27.710);
        endWayPoint.setLongitude(86.710);

        TrailSegment segment = new TrailSegment();
        segment.setStartWaypoint(wayPoint);
        segment.setEndWaypoint(endWayPoint);

        when(trackPointRepo.findFirstByLatitudeAndLongitude(anyDouble(), anyDouble())).thenReturn(Optional.empty());
        when(trackPointRepo.findPredecessorByCoordinates(eq(route.getId()), anyDouble(), anyDouble())).thenReturn(Optional.empty());

        List<TrailSegment> result = helper.generateAllTrailSegmentPaths(List.of(segment), route);

        assertNotNull(result.get(0).getPath());
        verify(gpxMergeHelper).assignTrackPointGlobalSequences(route.getId());
        verify(trackPointRepo, never()).findById(anyInt());
    }

    @Test
    void testGenerateTrailSegmentPath_NullGlobalSequences_ReturnsStraightLine() {
        TrackPoint start = TrackPoint.builder().latitude(27.700).longitude(86.700).globalSequence(null).build();
        TrackPoint end = TrackPoint.builder().latitude(27.710).longitude(86.710).build();

        LineString path = helper.generateTrailSegmentPath(start, end, route);

        assertNotNull(path);
        assertEquals(2, path.getNumPoints());
        verifyNoInteractions(trackPointRepo);
    }

    @Test
    void testGenerateTrailSegmentPath_NotEnoughPointsBetweenSequences_ReturnsStraightLine() {
        TrackPoint start = TrackPoint.builder().latitude(27.700).longitude(86.700).globalSequence(1).build();
        TrackPoint end = TrackPoint.builder().latitude(27.710).longitude(86.710).globalSequence(5).build();

        when(trackPointRepo.findBetweenGlobalSequences(route.getId(), 1, 5)).thenReturn(List.of(start));

        LineString path = helper.generateTrailSegmentPath(start, end, route);

        assertNotNull(path);
        assertEquals(2, path.getNumPoints());
    }

    @Test
    void testGenerateTrailSegmentPath_Success_ReturnsLineStringFromRepoPoints() {
        TrackPoint start = TrackPoint.builder().latitude(27.700).longitude(86.700).globalSequence(1).build();
        TrackPoint mid = TrackPoint.builder().latitude(27.705).longitude(86.705).globalSequence(2).build();
        TrackPoint end = TrackPoint.builder().latitude(27.710).longitude(86.710).globalSequence(3).build();

        when(trackPointRepo.findBetweenGlobalSequences(route.getId(), 1, 3)).thenReturn(List.of(start, mid, end));

        LineString path = helper.generateTrailSegmentPath(start, end, route);

        assertEquals(3, path.getNumPoints());
    }
}