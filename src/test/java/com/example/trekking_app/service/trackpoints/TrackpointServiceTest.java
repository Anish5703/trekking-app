package com.example.trekking_app.service.trackpoints;

import com.example.trekking_app.dto.geoJson.GeoJsonFeature;
import com.example.trekking_app.dto.geoJson.GeoJsonGeometry;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.gpx.GapPreviewDto;
import com.example.trekking_app.dto.route.FillGapsRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointBatchRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointCreateRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointResponse;
import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.exception.resource.ResourceDeletionFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.resource.ResourceUpdateFailedException;
import com.example.trekking_app.mapper.GeoJsonMapper;
import com.example.trekking_app.model.GpxSegmentStatus;
import com.example.trekking_app.model.RouteStatus;
import com.example.trekking_app.model.TrackPointStatus;
import com.example.trekking_app.repository.GpxSegmentRepository;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import com.example.trekking_app.service.gpx.GpxMergeService;
import com.example.trekking_app.service.graphHopper.TrailGapRoutingService;
import com.example.trekking_app.service.trailSegment.TrailSegmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrackpointServiceTest {

    @Mock
    private TrackPointRepository trackPointRepo;
    @Mock
    private RouteRepository routeRepo;
    @Mock
    private GpxMergeService gpxMergeService;
    @Mock
    private TrailSegmentService trailSegmentService;
    @Mock
    private GpxSegmentRepository gpxSegmentRepo;
    @Mock
    private GeoJsonMapper geoJsonMapper;
    @Mock
    private TrailGapRoutingService trailGapRoutingService;

    @InjectMocks
    private TrackPointService trackPointService;

    private Route route;
    private TrackPoint trackPoint ;
    private GpxSegment gpxSegment;
    private TrackPointRequest trackPointRequest;
    private TrackPointCreateRequest tpCreateReq;

    @BeforeEach
    void setUp() {

        GeometryFactory geometryFactory = new GeometryFactory();
        Point mockPoint = geometryFactory.createPoint(new Coordinate(81.2,23.3));

        route = new Route();
        route.setId(1);
        route.setRouteStatus(RouteStatus.MERGED);

        gpxSegment = new GpxSegment();
        gpxSegment.setId(11);

        trackPoint = TrackPoint.builder()
                .id(101)
                .route(route)
                .gpxSegment(gpxSegment)
                .latitude(23.3)
                .longitude(81.2)
                .localSequence(1)
                .globalSequence(1)
                .elevation(1200.0)
                .geom(mockPoint)
                .status(TrackPointStatus.ACTIVE)
                .build();

        trackPointRequest = TrackPointRequest.builder()
                .longitude(81.2)
                .latitude(24.4)
                .elevation(1200.0)
                .status(TrackPointStatus.ACTIVE)
                .build();

        tpCreateReq = TrackPointCreateRequest.builder()
                .latitude(23.3)
                .longitude(83.3)
                .precedingTpId(trackPoint.getId())
                .build();

    }

    @Test
    void testGetAllTrackPoints_Success(){
        List<TrackPoint> trackPoints = Collections.singletonList(trackPoint);
        Page<TrackPoint> trackPointPage = new PageImpl<>(trackPoints);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByRoute_IdOrderByGlobalSequenceAsc(eq(route.getId()),any(Pageable.class))).thenReturn(trackPointPage);

        ApiResponse<Page<TrackPointResponse>> response = trackPointService.getAllTrackPoints(route.getId(),0,10);

        assertNotNull(response);
        assertEquals(200,response.getStatus());

        Page<TrackPointResponse> resultPage = response.getData();
        assertFalse(resultPage.isEmpty());
        assertEquals(1, resultPage.getTotalElements());
        assertEquals(101, resultPage.getContent().get(0).getId());
    }

    @Test
    void testGetAllTrackPoints_RouteNotFound(){
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->trackPointService.getAllTrackPoints(route.getId(),0,10));
    }

    @Test
    void testGetTrackPointByLatAndLong_Success(){

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByRoute_IdAndLatitudeAndLongitude(route.getId(),23.3,81.2)).thenReturn(Optional.of(List.of(trackPoint)));

        ApiResponse<List<TrackPointResponse>> response = trackPointService.getTrackPointByLatAndLong(route.getId(),23.3,81.2);
        assertNotNull(response);
        assertEquals(200,response.getStatus());

        assertEquals(trackPoint.getId(),response.getData().get(0).getId());
        assertEquals(23.3,response.getData().get(0).getLatitude());
        assertEquals(81.2,response.getData().get(0).getLongitude());

    }

    @Test
    void testGetTrackPointByLatAndLong_RouteNotFound(){

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->trackPointService.getTrackPointByLatAndLong(route.getId(),23.3,81.2));
    }

    @Test
    void testGetTrackPointByLatAndLong_EmptyTrackPointResponseList(){

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByRoute_IdAndLatitudeAndLongitude(route.getId(),23.3,81.1)).thenReturn(Optional.of(Collections.emptyList()));

        assertThrows(ResourceNotFoundException.class,()->trackPointService.getTrackPointByLatAndLong(route.getId(),23.3,81.1));
    }

    @Test
    void testGetActiveTrackPoints_Success() {

        List<TrackPoint> trackPoints = Collections.singletonList(trackPoint);
        Page<TrackPoint> trackPointPage = new PageImpl<>(trackPoints);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceAsc(eq(route.getId()),any(Pageable.class))).thenReturn(trackPointPage);

        ApiResponse<Page<TrackPointResponse>> response = trackPointService.getActiveTrackPoints(route.getId(),0,10);

        assertNotNull(response);
        assertEquals(200,response.getStatus());
        assertEquals("active trackpoints fetched",response.getMessage());

        Page<TrackPointResponse> resultPage = response.getData();
        assertFalse(resultPage.isEmpty());
        assertEquals(1,resultPage.getTotalElements());
        assertEquals(101,resultPage.getContent().get(0).getId());
        assertEquals(23.3,resultPage.getContent().get(0).getLatitude());
    }

    @Test
    void testGetActiveTrackPoints_RouteNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->trackPointService.getActiveTrackPoints(route.getId(),0,10));
    }

    @Test
    void testGetInActiveTrackPoints_Success() {

        trackPoint.setStatus(TrackPointStatus.SOFT_DELETED);
        List<TrackPoint> trackPoints = Collections.singletonList(trackPoint);
        Page<TrackPoint> trackPointPage = new PageImpl<>(trackPoints);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByRoute_IdAndIsDeletedTrueOrderByUpdatedAtAsc(eq(route.getId()),any(Pageable.class))).thenReturn(trackPointPage);

        ApiResponse<Page<TrackPointResponse>> response = trackPointService.getInactiveTrackPoints(route.getId(),0,10);

        assertNotNull(response);
        assertEquals(200,response.getStatus());

        Page<TrackPointResponse> resultPage = response.getData();
        assertFalse(resultPage.isEmpty());
        assertEquals(1,resultPage.getTotalElements());
        assertEquals(TrackPointStatus.SOFT_DELETED,resultPage.getContent().get(0).getStatus());
    }

    @Test
    void testGetInActiveTrackPoints_RouteNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->trackPointService.getInactiveTrackPoints(route.getId(),0,10));
    }

    @Test
    void testUpdateTrackPoint_Success() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByIdAndRoute_Id(trackPoint.getId(), route.getId())).thenReturn(Optional.of(trackPoint));
        when(trackPointRepo.save(any(TrackPoint.class))).thenAnswer(i->i.getArgument(0));

        ApiResponse<TrackPointResponse> response = trackPointService.updateTrackPoint(route.getId(), trackPoint.getId(), trackPointRequest);
        assertNotNull(response);
        assertEquals(200,response.getStatus());

        verify(trailSegmentService).resolveAffectedSegments(route.getId(),trackPoint);

        TrackPointResponse resultResponse = response.getData();
        assertNotNull(resultResponse);
        assertEquals(trackPointRequest.getLongitude(),resultResponse.getLongitude());
        assertEquals(trackPointRequest.getLatitude(),resultResponse.getLatitude());
        assertEquals(trackPointRequest.getElevation(),resultResponse.getElevation());
        assertEquals(TrackPointStatus.ACTIVE,resultResponse.getStatus());
    }

    @Test
    void testUpdateTrackPoint_RouteNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->trackPointService.updateTrackPoint(route.getId(), trackPoint.getId(),trackPointRequest));
    }

    @Test
    void testUpdateTrackPoint_TrackPointNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByIdAndRoute_Id(trackPoint.getId(),route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->trackPointService.updateTrackPoint(route.getId(), trackPoint.getId(),trackPointRequest));
    }

    @Test
    void testUpdateTrackPoint_MergingStatusFail() {

        route.setRouteStatus(RouteStatus.MERGING);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByIdAndRoute_Id(trackPoint.getId(),route.getId())).thenReturn(Optional.of(trackPoint));

        assertThrows(ResourceUpdateFailedException.class,()->trackPointService.updateTrackPoint(route.getId(), trackPoint.getId(),trackPointRequest));
    }

    @Test
    void testUpdateTrackPoint_DBFail() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByIdAndRoute_Id(trackPoint.getId(), route.getId())).thenReturn(Optional.of(trackPoint));
        when(trackPointRepo.save(any(TrackPoint.class))).thenThrow(new DataAccessException("DB error"){});

        assertThrows(ResourceUpdateFailedException.class,()->trackPointService.updateTrackPoint(route.getId(),trackPoint.getId(),trackPointRequest));

    }

    @Test
    void testDeleteTrackPoint_Success() {

        trackPoint.setIsDeleted(false);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findById(trackPoint.getId())).thenReturn(Optional.of(trackPoint));

        ApiResponse<Void> response = trackPointService.deleteTrackPoint(route.getId(), trackPoint.getId());

        assertNotNull(response);
        assertNull(response.getData());
        assertEquals(TrackPointStatus.SOFT_DELETED,trackPoint.getStatus());
        assertTrue(trackPoint.getIsDeleted());

        verify(trackPointRepo).save(trackPoint);
        verify(gpxMergeService).mergeTrackPoints(route.getId());
    }

    @Test
    void testDeleteTrackPoint_RouteNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->trackPointService.deleteTrackPoint(route.getId(),trackPoint.getId()));
    }

    @Test
    void testDeleteTrackPoint_TrackPointNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findById(trackPoint.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->trackPointService.deleteTrackPoint(route.getId(), trackPoint.getId()));
    }

    @Test
    void testDeleteTrackPoint_DeletionFailed() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findById(trackPoint.getId())).thenReturn(Optional.of(trackPoint));
        when(trackPointRepo.save(any(TrackPoint.class))).thenThrow(new DataAccessException("Delete failed") {
        });

        assertThrows(ResourceDeletionFailedException.class,()->trackPointService.deleteTrackPoint(route.getId(), trackPoint.getId()));
    }

    @Test
    void testRecoverTrackPoint_Success()  {
        trackPoint.setIsDeleted(true);
        trackPoint.setStatus(TrackPointStatus.SOFT_DELETED);

        when(trackPointRepo.findByIdAndRoute_Id(trackPoint.getId(),route.getId())).thenReturn(Optional.of(trackPoint));
        when(trackPointRepo.save(any(TrackPoint.class))).thenAnswer(i->i.getArgument(0));

        ApiResponse<Void> response = trackPointService.recoverTrackPoint(route.getId(), trackPoint.getId());

        assertNotNull(response);
        assertFalse(trackPoint.getIsDeleted());
        assertEquals(TrackPointStatus.RECOVERED,trackPoint.getStatus());
    }

    @Test
    void testRecoverTrackPoint_TrackPointNotFound() {

        when(trackPointRepo.findByIdAndRoute_Id(trackPoint.getId(),route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->trackPointService.recoverTrackPoint(route.getId(),trackPoint.getId()));
    }

    @Test
    void testRecoverTrackPoint_TrackPointNotDeleted() {

        trackPoint.setIsDeleted(false);

        when(trackPointRepo.findByIdAndRoute_Id(trackPoint.getId(), route.getId())).thenReturn(Optional.of(trackPoint));

        assertThrows(ResourceUpdateFailedException.class,()->trackPointService.recoverTrackPoint(route.getId(), trackPoint.getId()));
    }

    @Test
    void testRecoverTrackPoints_Success(){

        TrackPointBatchRequest request = new TrackPointBatchRequest();
        request.setTrackPointIds(List.of(trackPoint.getId()));

        when(trackPointRepo.findAllByIdInAndRoute_IdAndIsDeletedTrueAndStatus(request.getTrackPointIds(),route.getId(),TrackPointStatus.SOFT_DELETED)).thenReturn(List.of(trackPoint));
        when(trackPointRepo.saveAll(List.of(trackPoint))).thenAnswer(i->i.getArgument(0));

        ApiResponse<Void> response = trackPointService.recoverTrackPoints(route.getId(),request);
        assertNotNull(response);
        assertEquals(200,response.getStatus());
        assertFalse(trackPoint.getIsDeleted());
        assertEquals(TrackPointStatus.RECOVERED,trackPoint.getStatus());

    }

    @Test
    void testRecoverTrackPoints_SizeNotMatched() {
        TrackPointBatchRequest req = new TrackPointBatchRequest();
        req.setTrackPointIds(List.of(1,2));

        when(trackPointRepo.findAllByIdInAndRoute_IdAndIsDeletedTrueAndStatus(anyList(),eq(route.getId()),eq(TrackPointStatus.SOFT_DELETED))).thenReturn(List.of(trackPoint));

        assertThrows(ResourceUpdateFailedException.class,()->trackPointService.recoverTrackPoints(route.getId(),req));
    }

    @Test
    void testDeleteListOfTrackPoint_Success() {
        TrackPointBatchRequest request = new TrackPointBatchRequest();
        request.setTrackPointIds(Collections.singletonList(trackPoint.getId()));
        trackPoint.setIsDeleted(false);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findById(trackPoint.getId())).thenReturn(Optional.of(trackPoint));

        ApiResponse<Void> response = trackPointService.deleteListOfTrackPoint(route.getId(),request);
        assertNotNull(response);
        assertEquals(200,response.getStatus());

        assertTrue(trackPoint.getIsDeleted());
        assertEquals(TrackPointStatus.SOFT_DELETED,trackPoint.getStatus());

        verify(trackPointRepo).saveAll(anyList());
        verify(gpxMergeService).mergeTrackPoints(route.getId());

    }

    @Test
    void testDeleteListOfTrackPoint_RouteNotFound(){

        TrackPointBatchRequest req = new TrackPointBatchRequest();
        req.setTrackPointIds(Collections.singletonList(trackPoint.getId()));
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->trackPointService.deleteListOfTrackPoint(route.getId(),req));
    }

    @Test
    void testDeleteListOfTrackPoint_RequestIsEmpty(){

        TrackPointBatchRequest req = new TrackPointBatchRequest();
        req.setTrackPointIds(Collections.emptyList());
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        assertThrows(ResourceDeletionFailedException.class,()->trackPointService.deleteListOfTrackPoint(route.getId(),req));

    }

    @Test
    void testDeleteListOfTrackPoint__TrackPointNotFound() {

        TrackPointBatchRequest req = new TrackPointBatchRequest();
        req.setTrackPointIds(Collections.singletonList(trackPoint.getId()));

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findById(trackPoint.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->trackPointService.deleteListOfTrackPoint(route.getId(),req));

    }

    @Test
    void testDeleteListOfTrackPoint_DBFail() {
        TrackPointBatchRequest req = new TrackPointBatchRequest();
        req.setTrackPointIds(Collections.singletonList(trackPoint.getId()));

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findById(trackPoint.getId())).thenReturn(Optional.of(trackPoint));
        when(trackPointRepo.saveAll(anyList())).thenThrow(new DataAccessException("DB ERROR") {
        });

        assertThrows(ResourceDeletionFailedException.class,()->trackPointService.deleteListOfTrackPoint(route.getId(),req));
    }

    @Test
    void testCreateTrackpoint_RouteLessThanOne() {
        route.setId(0);
        assertThrows(IllegalArgumentException.class,()->trackPointService.createTrackpoint(route.getId(),tpCreateReq));
    }

    @Test
    void testCreateTrackpoint_LatitudeLessThanOne() {
        tpCreateReq.setLatitude(0.0);
        assertThrows(IllegalArgumentException.class,()->trackPointService.createTrackpoint(route.getId(),tpCreateReq));
    }

    @Test
    void testCreateTrackpoint_LongitudeLessThanOne() {
        tpCreateReq.setLongitude(0.0);
        assertThrows(IllegalArgumentException.class,()->trackPointService.createTrackpoint(route.getId(),tpCreateReq));
    }
    @Test
    void createTrackpoint_WithPrecedingTrackpoint_Success() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findById(tpCreateReq.getPrecedingTpId())).thenReturn(Optional.of(trackPoint));

        ApiResponse<Void> response = trackPointService.createTrackpoint(route.getId(),tpCreateReq);

        assertNotNull(response);
        assertEquals(201,response.getStatus());
        verify(trackPointRepo).shiftLocalSequencesAfter(gpxSegment.getId(), trackPoint.getLocalSequence());

    }

    @Test
    void testCreateTrackpoint_WithoutPrecedingTrackPoint_CreatesNewGpxSegment() {
        TrackPointCreateRequest req = TrackPointCreateRequest.builder().latitude(27.71).longitude(86.52).build();

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findTopByRoute_IdAndSegmentStatusOrderByOrderIndexDesc(route.getId(), GpxSegmentStatus.TRACKPOINT))
                .thenReturn(Optional.empty());
        when(gpxSegmentRepo.save(any(GpxSegment.class))).thenAnswer(i -> i.getArgument(0));

        ApiResponse<Void> response = trackPointService.createTrackpoint(route.getId(), req);

        assertEquals(201, response.getStatus());

    }

    @Test
    void createTrackpoint_RouteNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->trackPointService.createTrackpoint(route.getId(),tpCreateReq));
    }

    @Test
    void createTrackpoint_TrackpointNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findById(tpCreateReq.getPrecedingTpId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->trackPointService.createTrackpoint(route.getId(),tpCreateReq));
    }

    @Test
    void testCreateTrackpoint_PrecedingTrackPointBelongsToDifferentRoute() {
        Route otherRoute = new Route();
        otherRoute.setId(999);
        TrackPoint foreignTp = TrackPoint.builder().id(500).route(otherRoute).localSequence(2).gpxSegment(gpxSegment).build();

        TrackPointCreateRequest req = TrackPointCreateRequest.builder().latitude(27.71).longitude(86.52).precedingTpId(foreignTp.getId()).build();

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findById(foreignTp.getId())).thenReturn(Optional.of(foreignTp));

        assertThrows(IllegalArgumentException.class, () -> trackPointService.createTrackpoint(route.getId(), req));
    }

    @Test
    void testDetectGaps_Success() {
        TrackPoint tp2 = TrackPoint.builder()
                .id(102)
                .route(route)
                .gpxSegment(gpxSegment)
                .latitude(23.4)
                .longitude(81.3)
                .build();
        gpxSegment.setSegmentStatus(GpxSegmentStatus.TRACKPOINT);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByRouteAndIsDeletedFalseOrderByGlobalSequenceAsc(route))
                .thenReturn(Optional.of(List.of(trackPoint, tp2)));

        List<GapPreviewDto> gaps = trackPointService.detectGaps(route.getId(), 200.0);

        assertNotNull(gaps);
        assertEquals(1, gaps.size());
        assertEquals(101, gaps.get(0).getPrecedingTpId());
        assertEquals(102, gaps.get(0).getFollowingTpId());
    }

    @Test
    void testDetectGaps_RouteNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> trackPointService.detectGaps(route.getId(), 200.0));
    }

    @Test
    void testDetectGaps_NoGapsFound() {
        TrackPoint tp2 = TrackPoint.builder()
                .id(102)
                .route(route)
                .gpxSegment(gpxSegment)
                .latitude(23.30001)
                .longitude(81.20001)
                .build();
        gpxSegment.setSegmentStatus(GpxSegmentStatus.TRACKPOINT);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByRouteAndIsDeletedFalseOrderByGlobalSequenceAsc(route))
                .thenReturn(Optional.of(List.of(trackPoint, tp2)));

        List<GapPreviewDto> gaps = trackPointService.detectGaps(route.getId(), 1000.0);

        assertTrue(gaps.isEmpty());
    }

    @Test
    void testFillGaps_Success() {
        TrackPoint tp2 = TrackPoint.builder()
                .id(102)
                .route(route)
                .gpxSegment(gpxSegment)
                .latitude(23.4)
                .longitude(81.3)
                .build();
        gpxSegment.setSegmentStatus(GpxSegmentStatus.TRACKPOINT);
        gpxSegment.setOrderIndex(1);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByRouteAndIsDeletedFalseOrderByGlobalSequenceAsc(route))
                .thenReturn(Optional.of(List.of(trackPoint, tp2)));

        FillGapsRequest request = new FillGapsRequest();
        request.setThresholdMeters(200.0);
        request.setPrecedingTpIds(List.of(101));

        when(trackPointRepo.findById(101)).thenReturn(Optional.of(trackPoint));

        GeoJsonGeometry geom = new GeoJsonGeometry();
        geom.setType("LineString");
        geom.setCoordinates(List.of(List.of(81.25, 23.35)));

        GeoJsonFeature feature = new GeoJsonFeature();
        feature.setGeometry(geom);

        ReflectionTestUtils.setField(geoJsonMapper,"trailGapRoutingService",trailGapRoutingService);

        when(trailGapRoutingService.findBridgePath(anyList(), anyList(), anyString()))
                .thenReturn(feature);
        when(gpxSegmentRepo.save(any(GpxSegment.class))).thenAnswer(i -> i.getArgument(0));

        ApiResponse<Void> response = trackPointService.fillGaps(route.getId(), request);

        assertEquals(201, response.getStatus());
        verify(trackPointRepo).saveAll(anyList());
        verify(trackPointRepo).updateGlobalSequences(route.getId());
    }

    @Test
    void testFillGaps_RouteNotFound() {
        FillGapsRequest request = new FillGapsRequest();

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> trackPointService.fillGaps(route.getId(), request));
    }

    @Test
    void testFillGaps_NoMatchingGaps() {
        TrackPoint tp2 = TrackPoint.builder()
                .id(102)
                .route(route)
                .gpxSegment(gpxSegment)
                .latitude(23.30001)
                .longitude(81.20001)
                .build();
        gpxSegment.setSegmentStatus(GpxSegmentStatus.TRACKPOINT);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByRouteAndIsDeletedFalseOrderByGlobalSequenceAsc(route))
                .thenReturn(Optional.of(List.of(trackPoint, tp2)));

        FillGapsRequest request = new FillGapsRequest();
        request.setThresholdMeters(1000.0);

        assertThrows(IllegalArgumentException.class, () -> trackPointService.fillGaps(route.getId(), request));
    }

    @Test
    void testFillGaps_BridgePathNull() {
        TrackPoint tp2 = TrackPoint.builder()
                .id(102)
                .route(route)
                .gpxSegment(gpxSegment)
                .latitude(23.4)
                .longitude(81.3)
                .build();
        gpxSegment.setSegmentStatus(GpxSegmentStatus.TRACKPOINT);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByRouteAndIsDeletedFalseOrderByGlobalSequenceAsc(route))
                .thenReturn(Optional.of(List.of(trackPoint, tp2)));

        FillGapsRequest request = new FillGapsRequest();
        request.setThresholdMeters(200.0);

        when(trackPointRepo.findById(101)).thenReturn(Optional.of(trackPoint));

        ReflectionTestUtils.setField(geoJsonMapper,"trailGapRoutingService",trailGapRoutingService);
        when(trailGapRoutingService.findBridgePath(anyList(), anyList(), anyString())).thenReturn(null);

        ApiResponse<Void> response = trackPointService.fillGaps(route.getId(), request);

        assertEquals(201, response.getStatus());
        verify(trackPointRepo, never()).saveAll(anyList());
    }
}

