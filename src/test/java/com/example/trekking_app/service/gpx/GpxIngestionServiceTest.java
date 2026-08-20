package com.example.trekking_app.service.gpx;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.gpx.GpxImportResponse;
import com.example.trekking_app.dto.gpx.GpxSegmentOrderRequest;
import com.example.trekking_app.dto.gpx.GpxSegmentResponse;
import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.exception.resource.*;
import com.example.trekking_app.model.GpxSegmentStatus;
import com.example.trekking_app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GpxIngestionServiceTest {

    @Mock
    private RouteRepository routeRepo;
    @Mock
    private GpxSegmentRepository gpxSegmentRepo;
    @Mock
    private GpxParserService gpxParserService;
    @Mock
    private TrackPointRepository trackPointRepo;
    @Mock
    private WayPointRepository wayPointRepo;
    @Mock
    private TrailSegmentRepository trailSegmentRepo;
    @Mock
    private POIRepository poiRepo;
    @Mock
    private AccommodationRepository accommodationRepo;
    @Mock
    private GpxMergeService gpxMergeService;

    @InjectMocks
    private GpxIngestionService gpxIngestionService;

    private Route route;
    private GpxSegment segment;

    @BeforeEach
    void setUp() {
        route = new Route();
        route.setId(1);
        route.setName("Annapurna Circuit");


        segment = new GpxSegment();
        segment.setId(50);
        segment.setOrderIndex(3);
        segment.setSourceFileName("existing.gpx");
        segment.setRoute(route);
    }

    private GpxImportResponse sampleImportResponse(int order) {
        return GpxImportResponse.builder()
                .routeId(route.getId())
                .segmentId(order)
                .segmentName("file" + order)
                .totalTrackPoints(2)
                .totalWayPoints(0)
                .totalDistanceInKm(1.5)
                .build();
    }

    @Test
    void testUploadGpxFiles_Success_TrackPoint_GpxFile_IncrementsOrderFromExisting() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "track1.gpx", "application/gpx+xml", "data".getBytes());

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findTopByRoute_IdAndSegmentStatusOrderByOrderIndexDesc(route.getId(), GpxSegmentStatus.TRACKPOINT))
                .thenReturn(Optional.of(segment));
        when(gpxParserService.parseTrackPoints(file, route, 4)).thenReturn(sampleImportResponse(4));

        ApiResponse<List<GpxImportResponse>> response =
                gpxIngestionService.uploadGpxFiles(route.getId(), List.of(file), GpxSegmentStatus.TRACKPOINT);

        assertEquals(201, response.getStatus());
        assertEquals(1, response.getData().size());
        assertTrue(response.getMessage().contains("trackpoints"));
        verify(gpxParserService).parseTrackPoints(file, route, 4);
        verify(gpxParserService, never()).parseTrackPointsFromExcel(any(), any(), any());
        verify(gpxMergeService).mergeTrackPoints(route.getId());
        verify(gpxMergeService, never()).mergeWayPoints(any());
    }

    @Test
    void testUploadGpxFiles_MultipleFiles_IncrementsOrderPerFile_MergesOnce() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("file", "a.gpx", "application/gpx+xml", "data".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "b.gpx", "application/gpx+xml", "data".getBytes());

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findTopByRoute_IdAndSegmentStatusOrderByOrderIndexDesc(route.getId(), GpxSegmentStatus.TRACKPOINT))
                .thenReturn(Optional.empty());
        when(gpxParserService.parseTrackPoints(file1, route, 1)).thenReturn(sampleImportResponse(1));
        when(gpxParserService.parseTrackPoints(file2, route, 2)).thenReturn(sampleImportResponse(2));

        ApiResponse<List<GpxImportResponse>> response =
                gpxIngestionService.uploadGpxFiles(route.getId(), List.of(file1, file2), GpxSegmentStatus.TRACKPOINT);

        assertEquals(2, response.getData().size());
        verify(gpxParserService).parseTrackPoints(file1, route, 1);
        verify(gpxParserService).parseTrackPoints(file2, route, 2);
        verify(gpxMergeService, times(1)).mergeTrackPoints(route.getId());
    }

    @Test
    void testUploadGpxFiles_Success_TrackPoint_ExcelFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "track.xlsx", "application/xlsx", "data".getBytes());

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findTopByRoute_IdAndSegmentStatusOrderByOrderIndexDesc(route.getId(), GpxSegmentStatus.TRACKPOINT))
                .thenReturn(Optional.empty());
        when(gpxParserService.parseTrackPointsFromExcel(file, route, 1)).thenReturn(sampleImportResponse(1));

        gpxIngestionService.uploadGpxFiles(route.getId(), List.of(file), GpxSegmentStatus.TRACKPOINT);

        verify(gpxParserService).parseTrackPointsFromExcel(file, route, 1);
        verify(gpxParserService, never()).parseTrackPoints(any(), any(), any());
    }

    @Test
    void testUploadGpxFiles_Success_WayPoint_GpxFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "way.gpx", "application/gpx+xml", "data".getBytes());

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findTopByRoute_IdAndSegmentStatusOrderByOrderIndexDesc(route.getId(), GpxSegmentStatus.WAYPOINT))
                .thenReturn(Optional.empty());
        when(gpxParserService.parseWayPoints(file, route, 1)).thenReturn(sampleImportResponse(1));

        ApiResponse<List<GpxImportResponse>> response =
                gpxIngestionService.uploadGpxFiles(route.getId(), List.of(file), GpxSegmentStatus.WAYPOINT);

        assertTrue(response.getMessage().contains("waypoints"));
        verify(gpxParserService).parseWayPoints(file, route, 1);
        verify(gpxMergeService).mergeWayPoints(route.getId());
        verify(gpxMergeService, never()).mergeTrackPoints(any());
    }

    @Test
    void testUploadGpxFiles_Success_WayPoint_ExcelFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "way.xlsx", "application/xlsx", "data".getBytes());

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findTopByRoute_IdAndSegmentStatusOrderByOrderIndexDesc(route.getId(), GpxSegmentStatus.WAYPOINT))
                .thenReturn(Optional.empty());
        when(gpxParserService.parseWayPointsFromExcel(file, route, 1)).thenReturn(sampleImportResponse(1));

        gpxIngestionService.uploadGpxFiles(route.getId(), List.of(file), GpxSegmentStatus.WAYPOINT);

        verify(gpxParserService).parseWayPointsFromExcel(file, route, 1);
        verify(gpxParserService, never()).parseWayPoints(any(), any(), any());
    }

    @Test
    void testUploadGpxFiles_RouteNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gpxIngestionService.uploadGpxFiles(route.getId(), List.of(), GpxSegmentStatus.TRACKPOINT));
        verifyNoInteractions(gpxParserService);
        verifyNoInteractions(gpxMergeService);
    }

    @Test
    void testGetAllGpxSegment_Success() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByRouteAndSegmentStatus(route, GpxSegmentStatus.TRACKPOINT))
                .thenReturn(Optional.of(List.of(segment)));

        ApiResponse<List<GpxSegmentResponse>> response =
                gpxIngestionService.getAllGpxSegment(route.getId(), GpxSegmentStatus.TRACKPOINT);

        assertEquals(200, response.getStatus());
        assertEquals(1, response.getData().size());
    }

    @Test
    void testGetAllGpxSegment_RouteNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gpxIngestionService.getAllGpxSegment(route.getId(), GpxSegmentStatus.TRACKPOINT));
    }

    @Test
    void testGetAllGpxSegment_SegmentsNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByRouteAndSegmentStatus(route, GpxSegmentStatus.TRACKPOINT))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gpxIngestionService.getAllGpxSegment(route.getId(), GpxSegmentStatus.TRACKPOINT));
    }

    @Test
    void testGetAllGpxSegment_EmptyList() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByRouteAndSegmentStatus(route, GpxSegmentStatus.TRACKPOINT))
                .thenReturn(Optional.of(List.of()));

        assertThrows(NoResourceFoundException.class,
                () -> gpxIngestionService.getAllGpxSegment(route.getId(), GpxSegmentStatus.TRACKPOINT));
    }


    @Test
    void testGetGpxSegment_Success() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByIdAndRoute_Id(segment.getId(), route.getId())).thenReturn(Optional.of(segment));

        ApiResponse<GpxSegmentResponse> response = gpxIngestionService.getGpxSegment(route.getId(), segment.getId());

        assertEquals(200, response.getStatus());
        assertNotNull(response.getData());
    }

    @Test
    void testGetGpxSegment_RouteNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gpxIngestionService.getGpxSegment(route.getId(), segment.getId()));
    }

    @Test
    void testGetGpxSegment_SegmentNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByIdAndRoute_Id(segment.getId(), route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gpxIngestionService.getGpxSegment(route.getId(), segment.getId()));
    }


    @Test
    void testReorderGpxSegment_Success_TrackPoint() {
        GpxSegmentOrderRequest request = new GpxSegmentOrderRequest();
        request.setSegmentIdWithOrder(Map.of(segment.getId(), 5));

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByRouteAndSegmentStatus(route, GpxSegmentStatus.TRACKPOINT))
                .thenReturn(Optional.of(List.of(segment)));

        ApiResponse<Void> response =
                gpxIngestionService.reorderGpxSegment(request, route.getId(), GpxSegmentStatus.TRACKPOINT);

        assertEquals(200, response.getStatus());
        assertEquals(5, segment.getOrderIndex());
        verify(gpxSegmentRepo).saveAll(List.of(segment));
        verify(gpxMergeService).mergeTrackPoints(route.getId());
        verify(gpxMergeService, never()).mergeWayPoints(any());
    }

    @Test
    void testReorderGpxSegment_Success_WayPoint() {
        GpxSegmentOrderRequest request = new GpxSegmentOrderRequest();
        request.setSegmentIdWithOrder(Map.of(segment.getId(), 7));

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByRouteAndSegmentStatus(route, GpxSegmentStatus.WAYPOINT))
                .thenReturn(Optional.of(List.of(segment)));

        gpxIngestionService.reorderGpxSegment(request, route.getId(), GpxSegmentStatus.WAYPOINT);

        assertEquals(7, segment.getOrderIndex());
        verify(gpxMergeService).mergeWayPoints(route.getId());
    }

    @Test
    void testReorderGpxSegment_RouteNotFound() {
        GpxSegmentOrderRequest request = new GpxSegmentOrderRequest();
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gpxIngestionService.reorderGpxSegment(request, route.getId(), GpxSegmentStatus.TRACKPOINT));
    }

    @Test
    void testReorderGpxSegment_SegmentsNotFound() {
        GpxSegmentOrderRequest request = new GpxSegmentOrderRequest();
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByRouteAndSegmentStatus(route, GpxSegmentStatus.TRACKPOINT))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gpxIngestionService.reorderGpxSegment(request, route.getId(), GpxSegmentStatus.TRACKPOINT));
    }


    @Test
    void testDeleteGpxSegment_Success_TrackPoint() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByIdAndRoute_Id(segment.getId(), route.getId())).thenReturn(Optional.of(segment));

        ApiResponse<Void> response =
                gpxIngestionService.deleteGpxSegment(segment.getId(), route.getId(), GpxSegmentStatus.TRACKPOINT);

        assertEquals(200, response.getStatus());
        verify(trackPointRepo).deleteAllByGpxSegment_Id(segment.getId());
        verify(gpxSegmentRepo).deleteById(segment.getId());
        verify(gpxMergeService).mergeTrackPoints(route.getId());
        verifyNoInteractions(wayPointRepo, trailSegmentRepo, poiRepo, accommodationRepo);
    }

    @Test
    void testDeleteGpxSegment_Success_WayPoint() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByIdAndRoute_Id(segment.getId(), route.getId())).thenReturn(Optional.of(segment));

        gpxIngestionService.deleteGpxSegment(segment.getId(), route.getId(), GpxSegmentStatus.WAYPOINT);

        verify(trailSegmentRepo).deleteByGpxSegment_Id(segment.getId());
        verify(poiRepo).deleteAllByWayPoint_GpxSegment_Id(segment.getId());
        verify(accommodationRepo).deleteByWayPoint_GpxSegment_Id(segment.getId());
        verify(wayPointRepo).deleteAllByGpxSegment_Id(segment.getId());
        verify(gpxMergeService).mergeWayPoints(route.getId());
        verifyNoInteractions(trackPointRepo);
    }

    @Test
    void testDeleteGpxSegment_RouteNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gpxIngestionService.deleteGpxSegment(segment.getId(), route.getId(), GpxSegmentStatus.TRACKPOINT));
    }

    @Test
    void testDeleteGpxSegment_SegmentNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByIdAndRoute_Id(segment.getId(), route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gpxIngestionService.deleteGpxSegment(segment.getId(), route.getId(), GpxSegmentStatus.TRACKPOINT));
    }

    @Test
    void testDeleteGpxSegment_DeletionFailedException() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByIdAndRoute_Id(segment.getId(), route.getId())).thenReturn(Optional.of(segment));
        doThrow(new RuntimeException("db error")).when(trackPointRepo).deleteAllByGpxSegment_Id(segment.getId());

        assertThrows(ResourceDeletionFailedException.class,
                () -> gpxIngestionService.deleteGpxSegment(segment.getId(), route.getId(), GpxSegmentStatus.TRACKPOINT));
    }


    @Test
    void testRemergeGpxSegment_Success_TrackPoint() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));

        ApiResponse<Void> response = gpxIngestionService.remergeGpxSegment(route.getId(), GpxSegmentStatus.TRACKPOINT);

        assertEquals(200, response.getStatus());
        verify(gpxMergeService).mergeTrackPoints(route.getId());
        verify(gpxMergeService, never()).mergeWayPoints(any());
    }

    @Test
    void testRemergeGpxSegment_Success_WayPoint() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));

        gpxIngestionService.remergeGpxSegment(route.getId(), GpxSegmentStatus.WAYPOINT);

        verify(gpxMergeService).mergeWayPoints(route.getId());
        verify(gpxMergeService, never()).mergeTrackPoints(any());
    }

    @Test
    void testRemergeGpxSegment_RouteNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gpxIngestionService.remergeGpxSegment(route.getId(), GpxSegmentStatus.TRACKPOINT));
    }
}