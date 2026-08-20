package com.example.trekking_app.service.xlsx;

import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrailSegment;
import com.example.trekking_app.exception.resource.ResourceDeletionFailedException;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import com.example.trekking_app.repository.AccommodationRepository;
import com.example.trekking_app.repository.POIRepository;
import com.example.trekking_app.repository.TrailSegmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComputeServiceTest {

    @Mock
    private POIRepository poiRepo;
    @Mock
    private AccommodationRepository accommodationRepo;
    @Mock
    private TrailSegmentRepository trailSegmentRepo;

    @InjectMocks
    private ComputeService computeService;

    private Route route;
    private POI poi;
    private Accommodation accommodation;
    private TrailSegment trailSegment;

    @BeforeEach
    void setUp() {
        route = new Route();
        route.setId(1);

        poi = new POI();
        poi.setId(10);

        accommodation = new Accommodation();
        accommodation.setId(20);

        trailSegment = new TrailSegment();
        trailSegment.setId(30);
    }

    @Test
    void testSaveAllPOI_Success() throws Exception {
        List<POI> pois = List.of(poi);
        when(poiRepo.saveAll(pois)).thenReturn(pois);

        CompletableFuture<Integer> result = computeService.saveAllPOI(pois);

        assertEquals(1, result.get());
        verify(poiRepo).saveAll(pois);
    }

    @Test
    void testSaveAllPOI_EmptyList() throws Exception {
        CompletableFuture<Integer> result = computeService.saveAllPOI(List.of());

        assertEquals(0, result.get());
        verify(poiRepo,never()).saveAll(anyList());
    }

    @Test
    void testSaveAllPOI_Exception() {
        List<POI> pois = List.of(poi);
        when(poiRepo.saveAll(pois)).thenThrow(new DataRetrievalFailureException("DB Error"));

        assertThrows(FileParsingFailedException.class, () -> computeService.saveAllPOI(pois));
    }

    @Test
    void testDeleteAllPOI_Success() throws Exception {
        route.setPois(List.of(poi));

        CompletableFuture<Integer> result = computeService.deleteAllPOI(route);

        assertEquals(1, result.get());
        verify(poiRepo).deleteAll(route.getPois());
    }

    @Test
    void testDeleteAllPOI_EmptyList() throws Exception {
        route.setPois(List.of());

        CompletableFuture<Integer> result = computeService.deleteAllPOI(route);

        assertEquals(0, result.get());
        verify(poiRepo,never()).deleteAll(anyList());
    }

    @Test
    void testDeleteAllPOI_Exception() {
        route.setPois(List.of(poi));
        doThrow(new DataRetrievalFailureException("DB Error")).when(poiRepo).deleteAll(anyList());

        assertThrows(ResourceDeletionFailedException.class, () -> computeService.deleteAllPOI(route));
    }

    @Test
    void testSaveAllAccommodation_Success() throws Exception {
        List<Accommodation> accommodations = List.of(accommodation);
        when(accommodationRepo.saveAll(accommodations)).thenReturn(accommodations);

        CompletableFuture<Integer> result = computeService.saveAllAccommodation(accommodations);

        assertEquals(1, result.get());
        verify(accommodationRepo).saveAll(accommodations);
    }

    @Test
    void testSaveAllAccommodation_EmptyList() throws Exception {
        CompletableFuture<Integer> result = computeService.saveAllAccommodation(List.of());

        assertEquals(0, result.get());
        verify(accommodationRepo,never()).saveAll(anyList());
    }

    @Test
    void testSaveAllAccommodation_Exception() {
        List<Accommodation> accommodations = List.of(accommodation);
        when(accommodationRepo.saveAll(accommodations)).thenThrow(new DataRetrievalFailureException("DB Error"));

        assertThrows(FileParsingFailedException.class, () -> computeService.saveAllAccommodation(accommodations));
    }

    @Test
    void testDeleteAllAccommodation_Success() throws Exception {
        route.setAccommodations(List.of(accommodation));

        CompletableFuture<Integer> result = computeService.deleteAllAccommodation(route);

        assertEquals(1, result.get());
        verify(accommodationRepo).deleteAll(route.getAccommodations());
    }

    @Test
    void testDeleteAllAccommodation_EmptyList() throws Exception {
        route.setAccommodations(List.of());

        CompletableFuture<Integer> result = computeService.deleteAllAccommodation(route);

        assertEquals(0, result.get());
        verifyNoInteractions(accommodationRepo);
    }

    @Test
    void testDeleteAllAccommodation_Exception() {
        route.setAccommodations(List.of(accommodation));
        doThrow(new DataRetrievalFailureException("DB Error")).when(accommodationRepo).deleteAll(anyList());

        assertThrows(ResourceDeletionFailedException.class, () -> computeService.deleteAllAccommodation(route));
    }

    @Test
    void testSaveAllTrailSegment_Success() throws Exception {
        List<TrailSegment> trailSegments = List.of(trailSegment);
        when(trailSegmentRepo.saveAll(trailSegments)).thenReturn(trailSegments);

        CompletableFuture<Integer> result = computeService.saveAllTrailSegment(trailSegments);

        assertEquals(1, result.get());
        verify(trailSegmentRepo).saveAll(trailSegments);
    }

    @Test
    void testSaveAllTrailSegment_EmptyList() throws Exception {
        CompletableFuture<Integer> result = computeService.saveAllTrailSegment(List.of());

        assertEquals(0, result.get());
        verifyNoInteractions(trailSegmentRepo);
    }

    @Test
    void testSaveAllTrailSegment_Exception() {
        List<TrailSegment> trailSegments = List.of(trailSegment);
        when(trailSegmentRepo.saveAll(trailSegments)).thenThrow(new DataRetrievalFailureException("DB Error"));

        assertThrows(FileParsingFailedException.class, () -> computeService.saveAllTrailSegment(trailSegments));
    }

    @Test
    void testDeleteAllTrailSegment_Success() throws Exception {
        route.setTrailSegments(List.of(trailSegment));

        CompletableFuture<Integer> result = computeService.deleteAllTrailSegment(route);

        assertEquals(1, result.get());
        verify(trailSegmentRepo).deleteAll(route.getTrailSegments());
    }

    @Test
    void testDeleteAllTrailSegment_EmptyList() throws Exception {
        route.setTrailSegments(List.of());

        CompletableFuture<Integer> result = computeService.deleteAllTrailSegment(route);

        assertEquals(0, result.get());
        verifyNoInteractions(trailSegmentRepo);
    }

    @Test
    void testDeleteAllTrailSegment_Exception() {
        route.setTrailSegments(List.of(trailSegment));
        doThrow(new DataRetrievalFailureException("DB Error")).when(trailSegmentRepo).deleteAll(anyList());

        assertThrows(ResourceDeletionFailedException.class, () -> computeService.deleteAllTrailSegment(route));
    }
}