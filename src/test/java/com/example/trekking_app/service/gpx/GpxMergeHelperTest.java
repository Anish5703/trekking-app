package com.example.trekking_app.service.gpx;

import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.model.RouteStatus;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.TrackPointRepository;
import com.example.trekking_app.repository.WayPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.LineString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GpxMergeHelperTest {

    @Mock
    private TrackPointRepository trackPointRepo;
    @Mock
    private RouteRepository routeRepo;
    @Mock
    private WayPointRepository wayPointRepo;

    @InjectMocks
    private GpxMergeHelper gpxMergeHelper;

    private Route route;
    private TrackPoint tp1;
    private TrackPoint tp2;
    private TrackPoint tp3;

    @BeforeEach
    void setUp() {
        route = new Route();
        route.setId(1);
        route.setName("Everest Base Camp");

        tp1 = TrackPoint.builder().latitude(27.700).longitude(86.700).build();
        tp2 = TrackPoint.builder().latitude(27.710).longitude(86.710).build();
        tp3 = TrackPoint.builder().latitude(27.720).longitude(86.720).build();
    }


    @Test
    void testAssignTrackPointGlobalSequences_Success() {
        gpxMergeHelper.assignTrackPointGlobalSequences(route.getId());

        verify(trackPointRepo).updateGlobalSequences(route.getId());
    }

    @Test
    void testAssignWayPointGlobalSequences_Success() {
        gpxMergeHelper.assignWayPointGlobalSequences(route.getId());

        verify(wayPointRepo).updateGlobalSequences(route.getId());
    }

    @Test
    void testFinalizeRoute_Success() {
        List<TrackPoint> trackPoints = List.of(tp1, tp2, tp3);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByRouteAndIsDeletedFalseOrderByGlobalSequenceAsc(route))
                .thenReturn(Optional.of(trackPoints));
        when(trackPointRepo.findMinElevation(route.getId())).thenReturn(Optional.of(1200.0));
        when(trackPointRepo.findMaxElevation(route.getId())).thenReturn(Optional.of(1800.0));
        when(routeRepo.save(any(Route.class))).thenAnswer(i -> i.getArgument(0));

        double expectedDist = Math.round(gpxMergeHelper.calculateTotalDistance(trackPoints) * 100.0) / 100.0;

        gpxMergeHelper.finalizeRoute(route.getId());

        assertEquals(1200.0, route.getMinElevation());
        assertEquals(1800.0, route.getMaxElevation());
        assertEquals(RouteStatus.MERGED, route.getRouteStatus());
        assertEquals(expectedDist, route.getDistanceInKm());
        assertNotNull(route.getPath());
        verify(routeRepo).save(route);
    }

    @Test
    void testFinalizeRoute_RouteNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> gpxMergeHelper.finalizeRoute(route.getId()));
    }

    @Test
    void testFinalizeRoute_NoActiveTrackPoints() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findByRouteAndIsDeletedFalseOrderByGlobalSequenceAsc(route))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> gpxMergeHelper.finalizeRoute(route.getId()));
    }

    @Test
    void testUpdateRouteStatus_Success() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(routeRepo.save(any(Route.class))).thenAnswer(i -> i.getArgument(0));

        gpxMergeHelper.updateRouteStatus(route.getId(), RouteStatus.FAILED);

        assertEquals(RouteStatus.FAILED, route.getRouteStatus());
        verify(routeRepo).save(route);
    }

    @Test
    void testUpdateRouteStatus_RouteNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gpxMergeHelper.updateRouteStatus(route.getId(), RouteStatus.FAILED));
    }

    @Test
    void testCalculateTotalDistance_EmptyList() {
        assertEquals(0.0, gpxMergeHelper.calculateTotalDistance(List.of()));
    }

    @Test
    void testCalculateTotalDistance_SinglePoint() {
        assertEquals(0.0, gpxMergeHelper.calculateTotalDistance(List.of(tp1)));
    }

    @Test
    void testCalculateTotalDistance_MultiplePoints() {
        double expected = gpxMergeHelper.haversineKm(tp1.getLatitude(), tp1.getLongitude(), tp2.getLatitude(), tp2.getLongitude())
                + gpxMergeHelper.haversineKm(tp2.getLatitude(), tp2.getLongitude(), tp3.getLatitude(), tp3.getLongitude());

        double actual = gpxMergeHelper.calculateTotalDistance(List.of(tp1, tp2, tp3));

        assertEquals(expected, actual, 0.0001);
    }

    @Test
    void testGenerateRoutePath_LessThanTwoPoints_ReturnsNull() {
        LineString path = gpxMergeHelper.generateRoutePath(List.of(tp1), route);

        assertNull(path);
        assertNull(route.getPath());
    }

    @Test
    void testGenerateRoutePath_TwoOrMorePoints_BuildsLineString() {
        LineString path = gpxMergeHelper.generateRoutePath(List.of(tp1, tp2, tp3), route);

        assertNotNull(path);
        assertEquals(3, path.getNumPoints());
        assertEquals(tp1.getLongitude(), path.getCoordinateN(0).x);
        assertEquals(tp1.getLatitude(), path.getCoordinateN(0).y);
    }

    @Test
    void testHaversineKm_SameCoordinates_ReturnsZero() {
        double dist = gpxMergeHelper.haversineKm(27.7, 86.7, 27.7, 86.7);

        assertEquals(0.0, dist, 0.0001);
    }

    @Test
    void testHaversineKm_KnownDistance() {
        double dist = gpxMergeHelper.haversineKm(27.7172, 85.3240, 28.2096, 83.9856);

        assertEquals(148.0, dist, 5.0);
    }
}