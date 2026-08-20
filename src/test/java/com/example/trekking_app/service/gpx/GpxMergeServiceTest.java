package com.example.trekking_app.service.gpx;

import com.example.trekking_app.exception.resource.ResourceMergeFailedException;
import com.example.trekking_app.model.RouteStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GpxMergeServiceTest {

    @Mock
    private GpxMergeHelper mergeHelper;

    @InjectMocks
    private GpxMergeService gpxMergeService;

    private static final Integer ROUTE_ID = 1;

    @Test
    void testMergeTrackPoints_Success() {
        doNothing().when(mergeHelper).updateRouteStatus(ROUTE_ID, RouteStatus.MERGING);
        doNothing().when(mergeHelper).assignTrackPointGlobalSequences(ROUTE_ID);
        doNothing().when(mergeHelper).finalizeRoute(ROUTE_ID);

        assertDoesNotThrow(() -> gpxMergeService.mergeTrackPoints(ROUTE_ID));

        verify(mergeHelper).updateRouteStatus(ROUTE_ID, RouteStatus.MERGING);
        verify(mergeHelper).assignTrackPointGlobalSequences(ROUTE_ID);
        verify(mergeHelper).finalizeRoute(ROUTE_ID);
        verify(mergeHelper, never()).updateRouteStatus(ROUTE_ID, RouteStatus.FAILED);
    }

    @Test
    void testMergeTrackPoints_MergeFailedException() {
        doNothing().when(mergeHelper).updateRouteStatus(ROUTE_ID, RouteStatus.MERGING);
        doThrow(new RuntimeException("sequence update failed"))
                .when(mergeHelper).assignTrackPointGlobalSequences(ROUTE_ID);
        doNothing().when(mergeHelper).updateRouteStatus(ROUTE_ID, RouteStatus.FAILED);

        assertThrows(ResourceMergeFailedException.class, () -> gpxMergeService.mergeTrackPoints(ROUTE_ID));

        verify(mergeHelper).updateRouteStatus(ROUTE_ID, RouteStatus.FAILED);
    }

    @Test
    void testMergeWayPoints_Success() {
        doNothing().when(mergeHelper).assignWayPointGlobalSequences(ROUTE_ID);

        assertDoesNotThrow(() -> gpxMergeService.mergeWayPoints(ROUTE_ID));

        verify(mergeHelper).assignWayPointGlobalSequences(ROUTE_ID);
    }

    @Test
    void testMergeWayPoints_MergeFailedException() {
        doThrow(new RuntimeException("sequence update failed"))
                .when(mergeHelper).assignWayPointGlobalSequences(ROUTE_ID);

        assertThrows(ResourceMergeFailedException.class, () -> gpxMergeService.mergeWayPoints(ROUTE_ID));
    }
}