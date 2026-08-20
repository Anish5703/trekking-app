package com.example.trekking_app.service.trackpoints;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.trackpoint.TrackPointBatchRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointRequest;
import com.example.trekking_app.dto.trackpoint.TrackPointResponse;
import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.exception.resource.ResourceDeletionFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.resource.ResourceUpdateFailedException;
import com.example.trekking_app.model.RouteStatus;
import com.example.trekking_app.model.TrackPointStatus;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import com.example.trekking_app.service.gpx.GpxMergeService;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @InjectMocks
    private TrackPointService trackPointService;

    private Route route;
    private TrackPoint trackPoint ;
    private GpxSegment gpxSegment;
    private TrackPointRequest trackPointRequest;

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
}
