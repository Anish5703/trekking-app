package com.example.trekking_app.service.trailSegment;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.trailSegment.TrailSegmentResponse;
import com.example.trekking_app.dto.trailSegment.TrailSegmentUpdateRequest;
import com.example.trekking_app.entity.*;
import com.example.trekking_app.exception.resource.NoResourceFoundException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.model.SegmentRefreshContext;
import com.example.trekking_app.model.TrailType;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import com.example.trekking_app.repository.TrailSegmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TrailSegmentServiceTest {

    @Mock
    private TrailSegmentRepository trailSegmentRepo;
    @Mock
    private RouteRepository routeRepo;
    @Mock
    private TrackPointRepository trackPointRepo;

    @InjectMocks
    private TrailSegmentService trailSegmentService;
    private TrailSegment trailSegment;
    private Route route ;
    private WayPoint startPoint;;
    private WayPoint endPoint;
    private GpxSegment gpxSegment;
    private TrackPoint trackPoint;

    @BeforeEach
    void setUp() {

        route = new Route();
        route.setId(1);

        gpxSegment = new GpxSegment();
        gpxSegment.setId(11);

        startPoint = new WayPoint();
        startPoint.setId(20);

        endPoint = new WayPoint();
        endPoint.setId(30);

        trailSegment = TrailSegment.builder()
                .id(101)
                .name("test name")
                .route(route)
                .type(TrailType.ROCKY_TRAIL)
                .startWaypoint(startPoint)
                .endWaypoint(endPoint)
                .color("red")
                .gpxSegment(gpxSegment)
                .estimatedTimeMinutes(180)
                .distanceInMeter(5000.0)
                .build();

        trackPoint = TrackPoint.builder()
                .id(100)
                .globalSequence(25)
                .build();

    }

    @Test
    void testGetAllTrailSegments_Success() {
        List<TrailSegment> trailSegments = Collections.singletonList(trailSegment);
        Page<TrailSegment> trailSegmentPage = new PageImpl<>(trailSegments);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trailSegmentRepo.findByRoute_Id(eq(route.getId()),any(Pageable.class))).thenReturn(trailSegmentPage);

        ApiResponse<Page<TrailSegmentResponse>> response = trailSegmentService.getAllTrailSegments(route.getId(),0,10);

        assertNotNull(response);
        assertEquals(200,response.getStatus());

        Page<TrailSegmentResponse> resultPage = response.getData();

        assertFalse(resultPage.isEmpty());
        assertEquals(trailSegment.getId(),resultPage.getContent().get(0).getId());
        assertEquals(trailSegment.getGpxSegment().getId(),resultPage.getContent().get(0).getGpxSegmentId());

    }

    @Test
    void testGetAllTrailSegments_RouteNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->trailSegmentService.getAllTrailSegments(route.getId(),0,10));
    }

    @Test
    void testGetAllTrailSegments_TrailSegmentIsEmpty() {
        List<TrailSegment> segmentList = Collections.emptyList();
        Page<TrailSegment> trailSegmentPage = new PageImpl<>(segmentList);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trailSegmentRepo.findByRoute_Id(eq(route.getId()),any(Pageable.class))).thenReturn(trailSegmentPage);

        assertThrows(NoResourceFoundException.class,()->trailSegmentService.getAllTrailSegments(route.getId(),0,10));
    }

    @Test
    void testGetTrailSegment_Success() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trailSegmentRepo.findByIdAndRoute_Id(trailSegment.getId(),route.getId())).thenReturn(Optional.of(trailSegment));

        ApiResponse<TrailSegmentResponse> response = trailSegmentService.getTrailSegment(route.getId(),trailSegment.getId());

        assertNotNull(response);
        assertEquals(200,response.getStatus());

        TrailSegmentResponse segmentResponse = response.getData();

        assertEquals(trailSegment.getId(),segmentResponse.getId());
        assertEquals(trailSegment.getGpxSegment().getId(),segmentResponse.getGpxSegmentId());

    }

    @Test
    void testGetTrailSegment_RouteNotFound() {
        when(routeRepo
                .findById(route.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->trailSegmentService.getTrailSegment(route.getId(),trailSegment.getId()));
    }

    @Test
    void testGetTrailSegment_TrailSegmentNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trailSegmentRepo.findByIdAndRoute_Id(trailSegment.getId(),route.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->trailSegmentService.getTrailSegment(route.getId(), trailSegment.getId()));
    }

    @Test
    void testUpdateTrailSegment_Success() {
        TrailSegmentUpdateRequest request = new TrailSegmentUpdateRequest();
        request.setName("new test name");
        request.setType(TrailType.WATER);
        request.setEstimatedTimeMinutes(120);
        request.setDistanceInMeter(1200.22);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trailSegmentRepo.findByIdAndRoute_Id(trailSegment.getId(),route.getId())).thenReturn(Optional.of(trailSegment));
        when(trailSegmentRepo.save(any(TrailSegment.class))).thenAnswer(i->i.getArgument(0));

        ApiResponse<TrailSegmentResponse> response = trailSegmentService.updateTrailSegment(route.getId(),trailSegment.getId(),request);

        assertNotNull(response);
        assertEquals(200,response.getStatus());

        TrailSegmentResponse segmentResponse = response.getData();
        assertEquals("new test name",segmentResponse.getName());
        assertEquals(request.getType(),segmentResponse.getType());
    }

    @Test
    void testUpdateTrailSegment_RouteNotFound() {
        TrailSegmentUpdateRequest request =new TrailSegmentUpdateRequest();
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->trailSegmentService.updateTrailSegment(route.getId(), trailSegment.getId(),request ));
    }

    @Test
    void testUpdateTrailSegment_TrailSegmentNotFound() {
        TrailSegmentUpdateRequest request = new TrailSegmentUpdateRequest();
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trailSegmentRepo.findByIdAndRoute_Id(trailSegment.getId(),route.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->trailSegmentService.updateTrailSegment(route.getId(), trailSegment.getId(),request));
    }

    @Test
    void testResolveAffectedSegments_Success() {
        startPoint.setLatitude(23.3);
        startPoint.setLongitude(81.2);
        endPoint.setLatitude(25.0);
        endPoint.setLongitude(83.0);

        TrackPoint boundaryStartTp = new TrackPoint();
        boundaryStartTp.setGlobalSequence(20);

        TrackPoint boundaryEndTp = new TrackPoint();
        boundaryEndTp.setGlobalSequence(30);

        when(trailSegmentRepo.findByRoute_Id(route.getId())).thenReturn(Collections.singletonList(trailSegment));
        when(trackPointRepo.findFirstByRoute_IdAndLatitudeAndLongitude(route.getId(), startPoint.getLatitude(), startPoint.getLongitude())).thenReturn(Optional.of(boundaryStartTp));
        when(trackPointRepo.findFirstByRoute_IdAndLatitudeAndLongitude(route.getId(), endPoint.getLatitude(), endPoint.getLongitude())).thenReturn(Optional.of(boundaryEndTp));

        List<SegmentRefreshContext> result = trailSegmentService.resolveAffectedSegments(route.getId(), trackPoint);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(trailSegment.getId(), result.get(0).segment().getId());
        assertEquals(boundaryStartTp, result.get(0).startTp());
        assertEquals(boundaryEndTp, result.get(0).endTp());
    }

    @Test
    void testResolveAffectedSegments_TrackPointHasNoSequence() {
        TrackPoint mockTp = new TrackPoint();
        mockTp.setGlobalSequence(null);

        List<SegmentRefreshContext> result = trailSegmentService.resolveAffectedSegments(route.getId(), mockTp);

        assertTrue(result.isEmpty());
    }

    @Test
    void testResolveAffectedSegments_NoSegmentsFound() {
        when(trailSegmentRepo.findByRoute_Id(route.getId())).thenReturn(Collections.emptyList());

        List<SegmentRefreshContext> result = trailSegmentService.resolveAffectedSegments(route.getId(), trackPoint);

        assertTrue(result.isEmpty());
    }

    @Test
    void testResolveAffectedSegments_BoundaryTrackPointsMissing() {
        startPoint.setLatitude(23.3);
        startPoint.setLongitude(81.2);
        endPoint.setLatitude(25.0);
        endPoint.setLongitude(83.0);

        when(trailSegmentRepo.findByRoute_Id(route.getId())).thenReturn(Collections.singletonList(trailSegment));
        when(trackPointRepo.findFirstByRoute_IdAndLatitudeAndLongitude(route.getId(), startPoint.getLatitude(), startPoint.getLongitude())).thenReturn(Optional.empty());

        List<SegmentRefreshContext> result = trailSegmentService.resolveAffectedSegments(route.getId(), trackPoint);

        assertTrue(result.isEmpty());
    }

    @Test
    void testResolveAffectedSegments_PointOutOfRange() {
        startPoint.setLatitude(23.3);
        startPoint.setLongitude(81.2);
        endPoint.setLatitude(25.0);
        endPoint.setLongitude(83.0);

        trackPoint.setGlobalSequence(50);

        TrackPoint boundaryStartTp = new TrackPoint();
        boundaryStartTp.setGlobalSequence(20);

        TrackPoint boundaryEndTp = new TrackPoint();
        boundaryEndTp.setGlobalSequence(30);

        when(trailSegmentRepo.findByRoute_Id(route.getId())).thenReturn(Collections.singletonList(trailSegment));
        when(trackPointRepo.findFirstByRoute_IdAndLatitudeAndLongitude(route.getId(), startPoint.getLatitude(), startPoint.getLongitude())).thenReturn(Optional.of(boundaryStartTp));
        when(trackPointRepo.findFirstByRoute_IdAndLatitudeAndLongitude(route.getId(), endPoint.getLatitude(), endPoint.getLongitude())).thenReturn(Optional.of(boundaryEndTp));

        List<SegmentRefreshContext> result = trailSegmentService.resolveAffectedSegments(route.getId(), trackPoint);

        assertTrue(result.isEmpty());
    }
}
