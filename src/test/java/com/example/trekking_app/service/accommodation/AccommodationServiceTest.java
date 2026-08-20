package com.example.trekking_app.service.accommodation;

import com.example.trekking_app.dto.accommodation.AccommodationRequest;
import com.example.trekking_app.dto.accommodation.AccommodationResponse;
import com.example.trekking_app.dto.accommodation.NearbyAccommodationProjection;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.NearbyRequest;
import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.Image;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.exception.resource.*;
import com.example.trekking_app.model.EntityType;
import com.example.trekking_app.repository.AccommodationRepository;
import com.example.trekking_app.repository.ImageRepository;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.service.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccommodationServiceTest {

    @Mock
    private AccommodationRepository accommodationRepo;
    @Mock
    private RouteRepository routeRepo;
    @Mock
    private ImageRepository imageRepo;
    @Mock
    private ImageService imageService;

    @InjectMocks
    private AccommodationService accommodationService;

    private Route route;
    private Accommodation accommodation;
    private AccommodationRequest accommodationRequest;
    private Image image;

    @BeforeEach
    void setUp() {
        route = new Route();
        route.setId(1);
        route.setName("Mountain city");

        accommodationRequest = new AccommodationRequest();
        accommodationRequest.setName("Teahouse A");
        accommodationRequest.setLatitude(27.8);
        accommodationRequest.setLongitude(86.7);

        accommodation = new Accommodation();
        accommodation.setId(10);
        accommodation.setName("Teahouse A");
        accommodation.setLatitude(27.8);
        accommodation.setLongitude(86.7);
        accommodation.setRoute(route) ;

        image = new Image();
        image.setId(100);
        image.setUrl("http://example.com/img.jpg");
        image.setEntityType(EntityType.ACCOMMODATION);
        image.setEntityId(10);
    }

    @Test
    void testCreateAccommodation_Success() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(accommodationRepo.existsByRoute_IdAndLongitudeAndLatitude(route.getId(), accommodationRequest.getLongitude(), accommodationRequest.getLatitude())).thenReturn(false);
        when(accommodationRepo.save(any(Accommodation.class))).thenAnswer(i -> {
            Accommodation acc = i.getArgument(0);
            acc.setId(10);
            return acc;
        });

        ApiResponse<AccommodationResponse> response = accommodationService.createAccommodation(route.getId(), accommodationRequest);

        assertNotNull(response);
        assertEquals(201, response.getStatus());
        assertEquals(accommodationRequest.getName(), response.getData().getName());
        assertEquals(route.getId(), response.getData().getRouteId());
    }

    @Test
    void testCreateAccommodation_DuplicateResource() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(accommodationRepo.existsByRoute_IdAndLongitudeAndLatitude(route.getId(), accommodationRequest.getLongitude(), accommodationRequest.getLatitude())).thenReturn(true);

        assertThrows(DuplicateResourceFoundException.class, () -> accommodationService.createAccommodation(route.getId(), accommodationRequest));
    }

    @Test
    void testCreateAccommodation_DataAccessException() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(accommodationRepo.existsByRoute_IdAndLongitudeAndLatitude(route.getId(), accommodationRequest.getLongitude(), accommodationRequest.getLatitude())).thenReturn(false);
        when(accommodationRepo.save(any(Accommodation.class))).thenThrow(new DataRetrievalFailureException("DB Error"));

        assertThrows(ResourceCreationFailedException.class, () -> accommodationService.createAccommodation(route.getId(), accommodationRequest));
    }

    @Test
    void testGetAccommodation_Success() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(accommodationRepo.findById(accommodation.getId())).thenReturn(Optional.of(accommodation));
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ACCOMMODATION, accommodation.getId())).thenReturn(Collections.singletonList(image));

        ApiResponse<AccommodationResponse> response = accommodationService.getAccommodation(route.getId(), accommodation.getId());

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals(accommodation.getId(), response.getData().getId());
        assertEquals(image.getUrl(), response.getData().getImageUrls().get(0));
    }

    @Test
    void testGetAccommodation_NotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(accommodationRepo.findById(accommodation.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accommodationService.getAccommodation(route.getId(), accommodation.getId()));
    }

    @Test
    void testGetAllAccommodation_Success() {
        Page<Accommodation> accommodationPage = new PageImpl<>(Collections.singletonList(accommodation));

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(accommodationRepo.findByRoute_Id(eq(route.getId()), any(Pageable.class))).thenReturn(accommodationPage);
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ACCOMMODATION, accommodation.getId())).thenReturn(Collections.singletonList(image));

        ApiResponse<Page<AccommodationResponse>> response = accommodationService.getAllAccommodation(route.getId(), 0, 10);

        assertNotNull(response);
        assertFalse(response.getData().isEmpty());
        assertEquals(accommodation.getId(), response.getData().getContent().get(0).getId());
    }

    @Test
    void testGetAllAccommodation_EmptyPage() {
        Page<Accommodation> emptyPage = new PageImpl<>(Collections.emptyList());

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(accommodationRepo.findByRoute_Id(eq(route.getId()), any(Pageable.class))).thenReturn(emptyPage);

        assertThrows(NoResourceFoundException.class, () -> accommodationService.getAllAccommodation(route.getId(), 0, 10));
    }

    @Test
    void testUpdateAccommodation_Success() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(accommodationRepo.findById(accommodation.getId())).thenReturn(Optional.of(accommodation));
        when(accommodationRepo.save(any(Accommodation.class))).thenAnswer(i -> i.getArgument(0));
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ACCOMMODATION, accommodation.getId())).thenReturn(Collections.singletonList(image));

        ApiResponse<AccommodationResponse> response = accommodationService.updateAccommodation(route.getId(), accommodation.getId(), accommodationRequest);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals(accommodationRequest.getName(), response.getData().getName());
    }

    @Test
    void testDeleteAccommodation_Success() throws Exception {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(accommodationRepo.findById(accommodation.getId())).thenReturn(Optional.of(accommodation));
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ACCOMMODATION, accommodation.getId())).thenReturn(Collections.singletonList(image));

        ApiResponse<Void> response = accommodationService.deleteAccommodation(route.getId(), accommodation.getId());

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        verify(accommodationRepo).delete(accommodation);
        verify(imageService).deleteImage(image.getId());
    }

    @Test
    void testGetAccommodationNearby_Success() {
        NearbyRequest request = new NearbyRequest();
        request.setLatitude(27.8);
        request.setLongitude(86.7);
        request.setRadiusMeters(5000.0);
        request.setLimit(10);

        NearbyAccommodationProjection projection = mock(NearbyAccommodationProjection.class);
        when(projection.getId()).thenReturn(accommodation.getId());

        when(accommodationRepo.findNearbyAccommodation(request.getLongitude(), request.getLatitude(), request.getRadiusMeters(), request.getLimit()))
                .thenReturn(Collections.singletonList(projection));
        when(accommodationRepo.findById(accommodation.getId())).thenReturn(Optional.of(accommodation));
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ACCOMMODATION, accommodation.getId())).thenReturn(Collections.emptyList());

        ApiResponse<List<AccommodationResponse>> response = accommodationService.getAccommodationNearby(request);

        assertNotNull(response);
        assertFalse(response.getData().isEmpty());
        assertEquals(accommodation.getId(), response.getData().get(0).getId());
    }

    @Test
    void testGetAccommodationNearby_NoResources() {
        NearbyRequest request = new NearbyRequest();
        request.setLatitude(27.8);
        request.setLongitude(86.7);
        request.setRadiusMeters(5000.0);
        request.setLimit(10);

        when(accommodationRepo.findNearbyAccommodation(request.getLongitude(),request.getLatitude(),request.getRadiusMeters(),request.getLimit())).thenReturn(Collections.emptyList());

        assertThrows(NoResourceFoundException.class, () -> accommodationService.getAccommodationNearby(request));
    }

    @Test
    void testGetListOfAccommodation_Success() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(accommodationRepo.findAllByRoute_Id(route.getId())).thenReturn(Collections.singletonList(accommodation));
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ACCOMMODATION, accommodation.getId())).thenReturn(Collections.singletonList(image));

        ApiResponse<List<AccommodationResponse>> response = accommodationService.getListOfAccommodation(route.getId());

        assertNotNull(response);
        assertFalse(response.getData().isEmpty());
        assertEquals(accommodation.getId(), response.getData().get(0).getId());
    }

    @Test
    void testGetListOfAccommodation_RouteNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()-> accommodationService.getListOfAccommodation(route.getId()));

    }

    @Test
    void testGetListOfAccommodation_ListOfResourceNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(accommodationRepo.findAllByRoute_Id(route.getId())).thenReturn(Collections.emptyList());

        assertThrows(NoResourceFoundException.class,()->accommodationService.getListOfAccommodation(route.getId()));
    }
}