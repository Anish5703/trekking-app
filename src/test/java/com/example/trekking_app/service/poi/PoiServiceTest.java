package com.example.trekking_app.service.poi;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.poi.NearbyPoiProjection;
import com.example.trekking_app.dto.poi.PoiRequest;
import com.example.trekking_app.dto.poi.PoiResponse;
import com.example.trekking_app.dto.route.NearbyRequest;
import com.example.trekking_app.entity.Image;
import com.example.trekking_app.entity.POI;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.exception.resource.NoResourceFoundException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.resource.ResourceUpdateFailedException;
import com.example.trekking_app.model.EntityType;
import com.example.trekking_app.model.POIType;
import com.example.trekking_app.repository.ImageRepository;
import com.example.trekking_app.repository.POIRepository;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.service.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PoiServiceTest {

    @Mock
    private POIRepository poiRepo;
    @Mock
    private RouteRepository routeRepo;
    @Mock private ImageRepository imageRepo;
    @Mock private ImageService imageService;
    @Mock private NearbyPoiProjection nearbyPoiProjection;

    @InjectMocks
    private PoiService poiService;
    private Route route;
    private POI newPOI;
    private PoiRequest request;

    @BeforeEach
    void setUp(){
        route = new Route();
        route.setId(1);

        request = new PoiRequest();
        request.setRouteId(route.getId());
        request.setName("testroute");
        request.setLatitude(11.5);
        request.setLongitude(12.5);
        request.setElevation(4.4);
        request.setType(POIType.RESTROOM);
        request.setDescription("test request");

        newPOI = new POI();
        newPOI.setId(1);
        newPOI.setRoute(route);
        newPOI.setWayPoint(null);
        newPOI.setName(request.getName());
        newPOI.setLatitude(request.getLatitude());
        newPOI.setLongitude(request.getLongitude());
        newPOI.setElevation(request.getElevation());
        newPOI.setType(request.getType());
        newPOI.setDescription(request.getDescription());


    }

    @Test
    void testCreatePoi_Success() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(poiRepo.save(any(POI.class))).thenReturn(newPOI);

        ApiResponse<PoiResponse> response = poiService.createPoi(route.getId(),request);
        assertEquals(201,response.getStatus());
    }

    @Test
    void testCreatePoi_ResourceNotFound(){

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->poiService.createPoi(route.getId(),request));
    }

    @Test
    void testUpdatePoi_Success() {
//  the poi and route id ,is mismatch ..
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(poiRepo.findByIdAndRouteId(route.getId(), newPOI.getId())).thenReturn(Optional.of(newPOI));
        when(poiRepo.save(any(POI.class
        ))).thenAnswer(i->i.getArgument(0));

        ApiResponse<PoiResponse> response = poiService.updatePoi(route.getId(), newPOI.getId(), request);

        assertNotNull(response);
        assertEquals(200,response.getStatus());
        assertEquals(newPOI.getName(),response.getData().getName());
    }

    @Test
    void testUpdatePoi_ResourceNotFoundOfRoute() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->poiService.updatePoi(route.getId(), newPOI.getId(),request));
    }

    @Test
    void testUpdatePoi_ResourceNotFoundOfPoi() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(poiRepo.findByIdAndRouteId(route.getId(), newPOI.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->poiService.updatePoi(route.getId(), newPOI.getId() ,request));
    }

    @Test
    void testUpdatePoi_ResourceUpdateFail() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(poiRepo.findByIdAndRouteId(route.getId(), newPOI.getId())).thenReturn(Optional.of(newPOI));
        when(poiRepo.save(any(POI.class))).thenThrow(new DataAccessException("db error while saving ") {
        });

        assertThrows(ResourceUpdateFailedException.class,()->poiService.updatePoi(route.getId(),newPOI.getId(),request));


    }

    @Test
    void testDeletePoi_Success() {

        when(poiRepo.findByIdAndRouteId(route.getId(), newPOI.getId())).thenReturn(Optional.of(newPOI));

        ApiResponse<Void> response = poiService.deletePoi(route.getId(), newPOI.getId());

        assertEquals(200,response.getStatus());
        assertEquals("poi deleted",response.getMessage());
        verify(poiRepo).delete(newPOI);

    }

    @Test
    void testDeletePoi_ResourceNotFound() {

        when(poiRepo.findByIdAndRouteId(route.getId(), newPOI.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->poiService.deletePoi(route.getId(), newPOI.getId()));
    }

    @Test
    void testGetAllPoi_Success() {
        List<POI> poiList = Collections.singletonList(newPOI);
        Page<POI>  poiPage = new PageImpl<>(poiList);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(poiRepo.findByRoute_id(eq(route.getId()),any(Pageable.class))).thenReturn(poiPage);

        ApiResponse<Page<PoiResponse>> poiResponse = poiService.getAllPoi(route.getId(),0,10);

        assertNotNull(poiResponse);
        assertEquals(200,poiResponse.getStatus());
        assertEquals(1,poiResponse.getData().getTotalPages());
        assertEquals(1,poiResponse.getData().getTotalElements());
    }

    @Test
    void testGetAllPoi_ThrowsResourceNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->poiService.getAllPoi(route.getId(),0,10));
    }

    @Test
    void testGetAllPoi_EmptyPoiResponse() {

        List<POI> poiList = Collections.emptyList();
        Page<POI> poiPage = new PageImpl<>(poiList);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(poiRepo.findByRoute_id(eq(route.getId()),any(Pageable.class))).thenReturn(poiPage);

        assertThrows(NoResourceFoundException.class,()->poiService.getAllPoi(route.getId(),0,10));

    }

    @Test
    void testGetPoi_Success() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(poiRepo.findByIdAndRouteId(newPOI.getId(), route.getId())).thenReturn(Optional.of(newPOI));

        ApiResponse<PoiResponse> response = poiService.getPoi(route.getId(), newPOI.getId());

        assertNotNull(response);
        assertEquals(200,response.getStatus());
        assertEquals(newPOI.getName(),response.getData().getName());
    }

    @Test
    void testGetPoi_RouteNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()-> poiService.getPoi(route.getId(), newPOI.getId()));
    }

    @Test
    void testGetPoi_PoiNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(poiRepo.findByIdAndRouteId(newPOI.getId(), route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->poiService.getPoi(route.getId(),newPOI.getId()));
    }

    @Test
    void testGetPoiNearby_Success() {
        NearbyRequest req = new NearbyRequest();
        req.setLatitude(23.3);
        req.setLongitude(80.8);
        req.setRadiusMeters(120.5);
        req.setLimit(4);

        when(nearbyPoiProjection.getId()).thenReturn(newPOI.getId());
        when(poiRepo.findNearbyPois(req.getLongitude(),req.getLatitude(),req.getRadiusMeters(),req.getLimit())).thenReturn(Collections.singletonList(nearbyPoiProjection));
        when(poiRepo.findById(newPOI.getId())).thenReturn(Optional.of(newPOI));
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.POI,newPOI.getId())).thenReturn(Collections.singletonList(new Image()));

        ApiResponse<List<PoiResponse>> response = poiService.getPoiNearby(req);
        assertNotNull(response);
        assertEquals(200,response.getStatus());
    }

    @Test
    void testGetPoiNearby_EmptyRequest() {

        NearbyRequest req = new NearbyRequest();
        req.setLatitude(23.3);
        req.setLongitude(80.8);
        req.setRadiusMeters(120.5);
        req.setLimit(4);


        when(poiRepo.findNearbyPois(req.getLongitude(),req.getLatitude(),req.getRadiusMeters(),req.getLimit())).thenReturn(Collections.emptyList());


        assertThrows(NoResourceFoundException.class,()->poiService.getPoiNearby(req));
    }

    @Test
    void testDeleteImagesForPoi_Success() {
        Image image = new Image();
        image.setId(1);

        poiService.deleteImagesForPoi(image);
        verify(imageService).deleteImage(1);

    }

    @Test
    void testDeleteImagesForPoi_EmptyImage(){
        Image image =new Image();
        image.setId(1);

        doThrow(new RuntimeException("Deletion failed")).when(imageService).deleteImage(1);

        assertDoesNotThrow(()->poiService.deleteImagesForPoi(image));
        
    }

}
