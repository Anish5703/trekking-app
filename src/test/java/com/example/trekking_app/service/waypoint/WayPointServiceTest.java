package com.example.trekking_app.service.waypoint;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.waypoint.WayPointRequest;
import com.example.trekking_app.dto.waypoint.WayPointResponse;
import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.WayPoint;
import com.example.trekking_app.exception.resource.ResourceDeletionFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.model.WayPointStatus;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.WayPointRepository;
import com.example.trekking_app.service.gpx.GpxMergeService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WayPointServiceTest {

    @Mock
    private WayPointRepository wayPointRepository;
    @Mock
    private RouteRepository routeRepository;
    @Mock
    private GpxMergeService gpxMergeService;

    @InjectMocks
    private WayPointService wayPointService;
    private WayPoint wayPoint;
    private Route route;
    private WayPointRequest wayPointRequest;
    private GpxSegment gpxSegment;

    @BeforeEach
    void setUp() {
        GeometryFactory geometryFactory = new GeometryFactory();
        Point mockPoint = geometryFactory.createPoint(new Coordinate(81.2,24.4));

        route = new Route();
        route.setId(1);

        gpxSegment = new GpxSegment();
        gpxSegment.setId(11);

        wayPoint = WayPoint.builder()
                .id(101)
                .route(route)
                .gpxSegment(gpxSegment)
                .latitude(23.3)
                .longitude(81.2)
                .localSequence(1)
                .globalSequence(1)
                .elevation(1200.0)
                .geom(mockPoint)
                .status(WayPointStatus.ACTIVE)
                .build();

        wayPointRequest = WayPointRequest.builder()
                .longitude(83)
                .latitude(25)
                .elevation(1300)
                .status(WayPointStatus.ACTIVE)
                .build();

    }
    @Test
    void testGetAllWayPoints_Success(){
        List<WayPoint> wayPointList = Collections.singletonList(wayPoint);
        Page<WayPoint> wayPointPage = new PageImpl<>(wayPointList);

        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        when(wayPointRepository.findByRoute_IdOrderByGlobalSequenceAsc(eq(route.getId()),any(Pageable.class))).thenReturn(wayPointPage);

        ApiResponse<Page<WayPointResponse>> response = wayPointService.getAllWayPoints(route.getId(),0,10);

        assertNotNull(response);
        assertEquals(200,response.getStatus());

        Page<WayPointResponse> resultPage = response.getData();

        assertFalse(resultPage.isEmpty());
        assertEquals(1,resultPage.getTotalElements());
        assertEquals(101,resultPage.getContent().get(0).getId());
        assertEquals(1200.0,resultPage.getContent().get(0).getElevation());
    }

    @Test
    void testGetAllWayPoints_RouteNotFound() {

        when(routeRepository.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->wayPointService.getAllWayPoints(route.getId(),0,10));
    }

    @Test
    void testGetActiveWayPoints_Success() {
        List<WayPoint> wayPointList = Collections.singletonList(wayPoint);
        Page<WayPoint> wayPointPage = new PageImpl<>(wayPointList);

        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        when(wayPointRepository.findByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceAsc(eq(route.getId()),any(Pageable.class))).thenReturn(wayPointPage);

        ApiResponse<Page<WayPointResponse>> response = wayPointService.getActiveWayPoints(route.getId(),0,10);

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        Page<WayPointResponse> resultPage = response.getData();

        assertFalse(resultPage.isEmpty());
        assertEquals(101,resultPage.getContent().get(0).getId());
        assertEquals(WayPointStatus.ACTIVE,resultPage.getContent().get(0).getStatus());
    }

    @Test
    void getActiveWayPoints_RouteNotFound() {

        when(routeRepository.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->wayPointService.getActiveWayPoints(route.getId(),0,10));
    }

    @Test
    void getInactiveWayPoints_Success() {
        wayPoint.setStatus(WayPointStatus.SOFT_DELETED);
        List<WayPoint> wayPointList = Collections.singletonList(wayPoint);
        Page<WayPoint> wayPointPage = new PageImpl<>(wayPointList);

        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        when(wayPointRepository.findByRoute_IdAndIsDeletedTrueOrderByUpdatedAtAsc(eq(route.getId()),any(Pageable.class))).thenReturn(wayPointPage);

        ApiResponse<Page<WayPointResponse>> response = wayPointService.getInactiveWayPoints(route.getId(),0,10);

        assertNotNull(response);
        assertEquals(200,response.getStatus());

        Page<WayPointResponse>  resultPage = response.getData();

        assertFalse(resultPage.isEmpty());
        assertEquals(101,resultPage.getContent().get(0).getId());
        assertEquals(WayPointStatus.SOFT_DELETED,resultPage.getContent().get(0).getStatus());
        assertEquals(gpxSegment.getId(),resultPage.getContent().get(0).getGpxSegmentId());
    }

    @Test
    void testGetInactiveWayPoints_RouteNotFound() {
        when(routeRepository.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->wayPointService.getInactiveWayPoints(route.getId(),0,10));

    }

    @Test
    void testDeleteWayPoint_Success() {

        wayPoint.setIsDeleted(false);
        wayPoint.setStatus(WayPointStatus.ACTIVE);

        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        when(wayPointRepository.findById(wayPoint.getId())).thenReturn(Optional.of(wayPoint));

        when(wayPointRepository.save(any(WayPoint.class))).thenAnswer(i->i.getArgument(0));

        ApiResponse<Void> response = wayPointService.deleteWayPoint(route.getId(),wayPoint.getId());

        assertNotNull(response);
        assertEquals(200,response.getStatus());
        assertEquals(WayPointStatus.SOFT_DELETED,wayPoint.getStatus());
        assertTrue(wayPoint.getIsDeleted());

        verify(wayPointRepository).save(wayPoint);
        verify(gpxMergeService).mergeWayPoints(route.getId());
    }

    @Test
    void testDeleteWayPoint_RouteNotFound() {
        when(routeRepository.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->wayPointService.deleteWayPoint(route.getId(),wayPoint.getId()));

    }

    @Test
    void testDeleteWayPoint_WaypointNotFound() {

        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        when(wayPointRepository.findById(wayPoint.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceDeletionFailedException.class,()->wayPointService.deleteWayPoint(route.getId(),wayPoint.getId()));
    }

    @Test
    void testDeleteWayPoint_ResourceDeletionFailed() {

        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        when(wayPointRepository.findById(wayPoint.getId())).thenReturn(Optional.of(wayPoint));
        when(wayPointRepository.save(any(WayPoint.class))).thenThrow(new DataAccessException("Deletion Failed") {
        });

        assertThrows(ResourceDeletionFailedException.class,()->wayPointService.deleteWayPoint(route.getId(),wayPoint.getId()));
    }

    @Test
    void testUpdateWayPoint_Success() {

        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        when(wayPointRepository.findByIdAndRoute_Id(wayPoint.getId(),route.getId())).thenReturn(Optional.of(wayPoint));
        when(wayPointRepository.save(any(WayPoint.class))).thenAnswer(i->i.getArgument(0));

        ApiResponse<WayPointResponse> response = wayPointService.updateWayPoint(route.getId(),wayPoint.getId(),wayPointRequest);

        assertNotNull(response);
        assertEquals("waypoint updated",response.getMessage());
        assertEquals(200,response.getStatus());

        WayPointResponse resultResponse = response.getData();

        assertEquals(wayPointRequest.getLatitude(),resultResponse.getLatitude());
        assertEquals(wayPointRequest.getElevation(),resultResponse.getElevation());
    }

    @Test
    void testUpdateWayPoint_RouteNotFound() {
        when(routeRepository.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->wayPointService.updateWayPoint(route.getId(),wayPoint.getId(),wayPointRequest));
    }

    @Test
    void testUpdateWayPoint_WaypointNotFound() {
        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        when(wayPointRepository.findByIdAndRoute_Id(wayPoint.getId(),route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->wayPointService.updateWayPoint(route.getId(),wayPoint.getId(),wayPointRequest));
    }
}
