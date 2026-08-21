package com.example.trekking_app.service.gpx;

import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.model.GpxSegmentStatus;
import com.example.trekking_app.repository.GpxSegmentRepository;
import com.example.trekking_app.repository.RouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GpxExportServiceTest {

    @Mock
    private RouteRepository routeRepo;
    @Mock
    private GpxSegmentRepository gpxSegmentRepo;

    @InjectMocks
    private GpxExportService gpxExportService;

    private GpxSegment segment;
    private Route route;

    @BeforeEach
    void setUp() {
        segment = new GpxSegment();
        segment.setId(5);
        segment.setSegmentStatus(GpxSegmentStatus.TRACKPOINT);

        route = new Route();
        route.setId(1);
    }

    @Test
    void testExportRouteAsGpx_RouteNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        assertThrows(ResourceNotFoundException.class,()->gpxExportService.exportRouteAsGpx(route.getId()));
    }

    @Test
    void testExportRouteAsGpx_SegmentNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByRoute_IdOrderByOrderIndexAsc(route.getId())).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,()->gpxExportService.exportRouteAsGpx(route.getId()));
    }

    @Test
    void exportSegmentAsGpx_RouteNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->gpxExportService.exportSegmentAsGpx(route.getId(),segment.getId()));
    }

    @Test
    void exportSegmentAsGpx_GpxSegmentNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByIdAndRoute_Id(segment.getId(),route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->gpxExportService.exportSegmentAsGpx(route.getId(),segment.getId()));
    }

    @Test
    void exportSegmentAsGpx_IncorrectSegmentStatus_ThrowsIllegalArgumentException() {

        segment.setSegmentStatus(GpxSegmentStatus.WAYPOINT);
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByIdAndRoute_Id(segment.getId(),route.getId())).thenReturn(Optional.of(segment));

        assertThrows(IllegalArgumentException.class,()->gpxExportService.exportSegmentAsGpx(route.getId(),segment.getId()));
    }

    @Test
    void exportSegmentAsGpx_NoActiveTrackPoint_ThrowsResourceNotFoundException() {
        segment.setTrackPoints(List.of());
        segment.setSegmentStatus(GpxSegmentStatus.TRACKPOINT);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(gpxSegmentRepo.findByIdAndRoute_Id(segment.getId(),route.getId())).thenReturn(Optional.of(segment));

        assertThrows(ResourceNotFoundException.class,()->gpxExportService.exportSegmentAsGpx(route.getId(),segment.getId()));
    }

    @Test
    void testGetSegmentOriginalFilename_ThrowsResourceNotFoundException() {
        when(gpxSegmentRepo.findByIdAndRoute_Id(segment.getId(), 1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> gpxExportService.getSegmentOriginalFilename(1, segment.getId()));
    }

    @Test
    void testGetSegmentOriginalFilename_NullSourceFileName_ReturnsFallbackName() {
        segment.setSourceFileName(null);
        when(gpxSegmentRepo.findByIdAndRoute_Id(segment.getId(), 1)).thenReturn(Optional.of(segment));

        String result = gpxExportService.getSegmentOriginalFilename(1, segment.getId());

        assertEquals("segment-5.gpx", result);
    }

    @Test
    void testGetSegmentOriginalFilename_BlankSourceFileName_ReturnsFallbackName() {
        segment.setSourceFileName("   ");
        when(gpxSegmentRepo.findByIdAndRoute_Id(segment.getId(), 1)).thenReturn(Optional.of(segment));

        String result = gpxExportService.getSegmentOriginalFilename(1, segment.getId());

        assertEquals("segment-5.gpx", result);
    }

    @Test
    void testGetSegmentOriginalFilename_NameAlreadyEndsWithGpx_ReturnsAsIs() {
        segment.setSourceFileName("trail-day1.gpx");
        when(gpxSegmentRepo.findByIdAndRoute_Id(segment.getId(), 1)).thenReturn(Optional.of(segment));

        String result = gpxExportService.getSegmentOriginalFilename(1, segment.getId());

        assertEquals("trail-day1.gpx", result);
    }

    @Test
    void testGetSegmentOriginalFilename_NameEndsWithUppercaseExtension_ReturnsAsIs() {
        segment.setSourceFileName("trail-day1.GPX");
        when(gpxSegmentRepo.findByIdAndRoute_Id(segment.getId(), 1)).thenReturn(Optional.of(segment));

        String result = gpxExportService.getSegmentOriginalFilename(1, segment.getId());

        assertEquals("trail-day1.GPX", result);
    }

    @Test
    void testGetSegmentOriginalFilename_NameWithoutExtension_AppendsGpxExtension() {
        segment.setSourceFileName("trail-day1");
        when(gpxSegmentRepo.findByIdAndRoute_Id(segment.getId(), 1)).thenReturn(Optional.of(segment));

        String result = gpxExportService.getSegmentOriginalFilename(1, segment.getId());

        assertEquals("trail-day1.gpx", result);
    }
}