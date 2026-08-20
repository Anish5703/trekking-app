package com.example.trekking_app.service.xlsx;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.poi.XlsxImportResponse;
import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrailSegment;
import com.example.trekking_app.entity.WayPoint;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.repository.RouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class XlsxIngestionServiceTest {

    @Mock
    private XlsxParser parser;
    @Mock
    private RouteRepository routeRepo;
    @Mock
    private ComputeService computeService;

    @InjectMocks
    private XlsxIngestionService xlsxIngestionService;

    private Route route;
    private MockMultipartFile file;

    @BeforeEach
    void setUp() {
        route = new Route();
        route.setId(1);
        route.setName("Annapurna Circuit");

        file = new MockMultipartFile("file", "survey.xlsx", "application/xlsx", "data".getBytes());
    }

    @Test
    void testUploadXlsx_Success() {
        WayPoint wayPoint = new WayPoint();
        POI poi = new POI();
        Accommodation accommodation = new Accommodation();
        TrailSegment trailSegment = new TrailSegment();

        ParseOutput parseOutput = new ParseOutput(List.of(), List.of(wayPoint), List.of(poi), List.of(accommodation), List.of(trailSegment)
        );

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(parser.parse(file, route)).thenReturn(parseOutput);
        when(computeService.saveAllPOI(parseOutput.pois())).thenReturn(CompletableFuture.completedFuture(1));
        when(computeService.saveAllAccommodation(parseOutput.accommodations())).thenReturn(CompletableFuture.completedFuture(1));
        when(computeService.saveAllTrailSegment(parseOutput.trailSegments())).thenReturn(CompletableFuture.completedFuture(1));

        ApiResponse<XlsxImportResponse> response = xlsxIngestionService.uploadXlsx(route.getId(), file);

        assertEquals(200, response.getStatus());
        assertEquals(1, response.getData().getNumberOfPOI());
        assertEquals(1, response.getData().getNumberOfAccommodation());
        assertEquals(1, response.getData().getNumberOfTrailSegment());
        assertEquals(1, response.getData().getNumberOfWayPoint());
        assertEquals(0, response.getData().getNumberOfRows());
    }

    @Test
    void testUploadXlsx_RouteNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> xlsxIngestionService.uploadXlsx(route.getId(), file));
        verifyNoInteractions(parser, computeService);
    }

    @Test
    void testDeleteXlsxImports_Success() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(computeService.deleteAllPOI(route)).thenReturn(CompletableFuture.completedFuture(2));
        when(computeService.deleteAllAccommodation(route)).thenReturn(CompletableFuture.completedFuture(1));
        when(computeService.deleteAllTrailSegment(route)).thenReturn(CompletableFuture.completedFuture(3));

        ApiResponse<Void> response = xlsxIngestionService.deleteXlsxImports(route.getId());

        assertEquals(200, response.getStatus());
        verify(computeService).deleteAllPOI(route);
        verify(computeService).deleteAllAccommodation(route);
        verify(computeService).deleteAllTrailSegment(route);
    }

    @Test
    void testDeleteXlsxImports_RouteNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> xlsxIngestionService.deleteXlsxImports(route.getId()));
        verifyNoInteractions(computeService);
    }
}