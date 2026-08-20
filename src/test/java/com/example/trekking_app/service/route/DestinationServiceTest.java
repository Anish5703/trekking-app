package com.example.trekking_app.service.route;

import com.example.trekking_app.dto.destination.DestinationRequest;
import com.example.trekking_app.dto.destination.DestinationResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.entity.Destination;
import com.example.trekking_app.entity.Image;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.exception.resource.*;
import com.example.trekking_app.model.EntityType;
import com.example.trekking_app.repository.DestinationRepository;
import com.example.trekking_app.repository.ImageRepository;
import com.example.trekking_app.service.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DestinationServiceTest {
    @Mock
    DestinationRepository destinationRepo;
    @Mock
    ImageRepository imageRepo;
    @Mock
    ImageService imageService;
    private Destination destination;
    private DestinationRequest request;
    private Image image;

    @InjectMocks
    DestinationService destinationService;

    @BeforeEach
    void setUp() {
        image = new Image();
        image.setId(11);
        image.setUrl("url");

        destination = new Destination();
        destination.setId(1);
        destination.setName("Lumbini palace");
        destination.setDistrict("Rupandehi");
        destination.setRegion("Lumbini");

        request =new DestinationRequest();
        request.setDistrict("Rupandehi");
        request.setName("Lumbini Palace");
        request.setRegion("Lumbini");

    }


    @Test
    void getAllDestinationSuccess() {

        when(destinationRepo.findAll()).thenReturn(List.of(destination));
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.DESTINATION,destination.getId())).thenReturn(List.of(image));

        ApiResponse<List<DestinationResponse>> response = destinationService.getAllDestination();

        assertNotNull(response);
        assertEquals(200,response.getStatus());

        List<DestinationResponse> responselist = response.getData();
        assertEquals(destination.getId(),responselist.get(0).getId());
        assertEquals(Collections.singletonList(image.getUrl()),responselist.get(0).getImageUrls());

    }

    @Test
    void testGetAllDestination_DestinationListIsEmpty() {
        when(destinationRepo.findAll()).thenReturn(List.of());

        assertThrows(NoResourceFoundException.class,()->destinationService.getAllDestination());
    }

    @Test
    void createDestinationSuccess(){

        when(destinationRepo.existsByNameAndDistrictAndRegion(request.getName(),request.getDistrict(),request.getRegion())).thenReturn(false);

        when(destinationRepo.save(any(Destination.class))).thenReturn(destination);

        ApiResponse<DestinationResponse> response = destinationService.createDestination(request);

        assertNotNull(response);
        assertEquals(201,response.getStatus());

        DestinationResponse result = response.getData();
        assertEquals(destination.getId(),result.getId());
        assertEquals(destination.getRegion(),result.getRegion());
        assertTrue(result.getImageUrls().isEmpty());

    }

    @Test
    void testCreateDestination_ThrowsDuplicateResourceFoundException() {

        when(destinationRepo.existsByNameAndDistrictAndRegion(request.getName(),request.getDistrict(),request.getRegion())).thenReturn(true);

        assertThrows(DuplicateResourceFoundException.class,()->destinationService.createDestination(request));
    }

    @Test
    void testCreateDestination_ResourceCreationFailure(){

        when(destinationRepo.existsByNameAndDistrictAndRegion(request.getName(),request.getDistrict(),request.getRegion())).thenReturn(false);
        when(destinationRepo.save(any())).thenThrow(new DataAccessException("db error") {
        });
        assertThrows(ResourceCreationFailedException.class,()->destinationService.createDestination(request));

    }

    @Test
    void updateDestination_Success() {

        request.setName("updated name");

        when(destinationRepo.findById(1)).thenReturn(Optional.of(destination));

        when(destinationRepo.save(any(Destination.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<DestinationResponse> response = destinationService.updateDestination(request,1);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("updated name",response.getData().getName());

        verify(destinationRepo).save(destination);
    }

    @Test
    void testUpdateDestination_WhenResourceNotFound() {

        when(destinationRepo.findById(destination.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->destinationService.updateDestination(request,destination.getId()));
    }

    @Test
    void testUpdateDestination_DoesNotUpdateResource(){
        request.setName("Updated:New lumbini");

        when(destinationRepo.findById(destination.getId())).thenReturn(Optional.of(destination));
        when(destinationRepo.save(any(Destination.class))).thenThrow(new DataAccessException("db error ") {
        });

        assertThrows(ResourceUpdateFailedException.class,()->destinationService.updateDestination(request,destination.getId()));
    }

    @Test
    void deleteDestination_Success() {
        destination.setRoutes(Collections.emptyList());
        when(destinationRepo.findById(1)).thenReturn(Optional.of(destination));
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.DESTINATION,destination.getId())).thenReturn(List.of(image));

        ApiResponse<Void> response = destinationService.deleteDestination(destination.getId());

        assertEquals("destination deleted",response.getMessage());
        assertEquals(200,response.getStatus());
        verify(imageService).deleteImage(image.getId());
    }

    @Test
    void testDeleteDestination_WhenResourceNotFound() {
        when(destinationRepo.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->destinationService.deleteDestination(1));
    }

    @Test
    void testDeleteDestination_EmptyImage(){
        destination.setRoutes(Collections.emptyList());

        when(destinationRepo.findById(destination.getId())).thenReturn(Optional.of(destination));
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.DESTINATION,destination.getId())).thenReturn(Collections.emptyList());

        ApiResponse<Void> response = destinationService.deleteDestination(destination.getId());
        assertNotNull(response);
        verify(imageService,never()).deleteImage(any());
    }

    @Test
    void testDeleteDestination_RouteListIsNotEmpty() {
        // !false=true, destination.getRoute()-list .empty should be false,it is not empty,so error is thrown
        destination.setRoutes(List.of(new Route()));

        when(destinationRepo.findById(1)).thenReturn(Optional.of(destination));

        assertThrows(ResourceDeletionFailedException.class,()->destinationService.deleteDestination(1));
    }

    @Test
    void testDeleteImagesForDestination(){
        destinationService.deleteImagesForDestination(image);
        verify(imageService).deleteImage(image.getId());
    }

    @Test
    void testDeleteImagesForDestination_EmptyImage(){
        doThrow(new RuntimeException("Deletion failed")).when(imageService).deleteImage(image.getId());
        assertDoesNotThrow(()->destinationService.deleteImagesForDestination(image));
    }

    @Test
    void getDestinationByIdSuccessfully() {

        Image image = new Image();
        image.setEntityId(1);
        image.setEntityType(EntityType.DESTINATION);
        image.setUrl("url");

        when(destinationRepo.findById(destination.getId())).thenReturn(Optional.of(destination));
        when(imageRepo.findByEntityTypeAndEntityId(image.getEntityType(),destination.getId())).thenReturn(List.of(image));

        ApiResponse<DestinationResponse> response =  destinationService.getDestination(destination.getId());

        assertNotNull(response);
        assertEquals(200,response.getStatus());
    }

    @Test
    void testGetDestinationByIdThrowsResourceNotFoundException() {

        when(destinationRepo.findById(destination.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->destinationService.getDestination(destination.getId()));

    }

}
