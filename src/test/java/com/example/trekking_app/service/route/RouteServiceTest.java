package com.example.trekking_app.service.route;

import com.example.trekking_app.dto.geoJson.GeoJsonFeatureCollection;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.*;
import com.example.trekking_app.entity.*;
import com.example.trekking_app.exception.auth.UserNotFoundException;
import com.example.trekking_app.exception.resource.NoResourceFoundException;
import com.example.trekking_app.exception.resource.ResourceDeletionFailedException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.route.CreateRouteFailedException;
import com.example.trekking_app.mapper.GeoJsonMapper;
import com.example.trekking_app.model.DifficultyLevel;
import com.example.trekking_app.model.EntityType;
import com.example.trekking_app.model.Role;
import com.example.trekking_app.model.RouteStatus;
import com.example.trekking_app.repository.*;
import com.example.trekking_app.service.graphHopper.TrailGapRoutingService;
import com.example.trekking_app.service.image.ImageService;
import com.example.trekking_app.service.overpass.ExternalPoiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RouteServiceTest {
    @Mock
    private RouteRepository routeRepo;
    @Mock
    private DestinationRepository destinationRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private GpxSegmentRepository gpxSegmentRepo;
    @Mock
    private RecentlyViewedRepository recentlyViewedRepository;
    @Mock
    private TrackPointRepository trackPointRepo;
    @Mock private  ImageRepository imageRepo;
    @Mock private ImageService imageService;
    @Mock private  NearbyRouteProjection nearbyRouteProjection;
    @Mock private GeoJsonMapper geoJsonMapper;

    @InjectMocks
    private RouteService routeService;

    private  Route route;
    private TrackPoint trackPoint;
    private Destination destination;
    private User user;
    private Image image;
    private RouteRequest req;
    private NearbyRequest nearbyRequest;
    private  RecentlyViewed recentlyViewed;

    @BeforeEach
    void setUp() {

        GeometryFactory geometryFactory = new GeometryFactory();
        Point mockPoint = geometryFactory.createPoint(new Coordinate(81.2,23.3));

        Coordinate[] coordinates = new Coordinate[]{
                new Coordinate(81.25,23.33),
                new Coordinate(81.20,23.35)
        };

        LineString mockPath = geometryFactory.createLineString(coordinates);

        image = new Image();
        image.setId(2);
        image.setUrl("url");

        user = new User();
        user.setId(10);
        user.setRole(Role.CUSTOMER);

        destination = new Destination();
        destination.setId(11);

        route = new Route();
        route.setId(1);
        route.setName("Route 1");
        route.setDestination(destination);
        route.setIsPublished(true);
        route.setPath(mockPath);
        route.setMinElevation(1340.34);
        route.setMaxElevation(1500.00);
        route.setDistanceInKm(16.6);

        trackPoint = new TrackPoint();
        trackPoint.setId(111);
        trackPoint.setGeom(mockPoint);

        req = new RouteRequest();
        req.setName("Route 1");
        req.setDestinationId(destination.getId());
        req.setDescription("this is a route description");
        req.setDifficultyLevel(DifficultyLevel.MODERATE);

        nearbyRequest = NearbyRequest.builder()
                .longitude(83.3)
                .latitude(25.5)
                .radiusMeters(2200.3)
                .limit(18)
                .build();

        recentlyViewed = RecentlyViewed.builder()
                .id(5)
                .user(user)
                .route(route)
                .counter(5)
                .build();
    }

    @Test
    void testGetRoute_Success() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));
        when(trackPointRepo.findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceAsc(route.getId())).thenReturn(Optional.of(trackPoint));
        when(trackPointRepo.findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceDesc(route.getId())).thenReturn(Optional.of(trackPoint));
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ROUTE,route.getId())).thenReturn(List.of(image));

        ApiResponse<RouteResponse> response = routeService.getRoute(route.getId(),user.getId());

        assertNotNull(response);
        assertEquals(200,response.getStatus());

        RouteResponse resultResponse = response.getData();
        assertNotNull(resultResponse);
        assertEquals(route.getId(),resultResponse.getId());
        assertEquals("Route 1",resultResponse.getName());
        assertTrue(resultResponse.getIsPublished());
    }

    @Test
    void testGetRoute_IdLessThanOne() {

        route.setId(0);

        assertThrows(IllegalArgumentException.class,()->routeService.getRoute(0,user.getId()));
    }

    @Test
    void testGetRoute_RouteNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->routeService.getRoute(route.getId(),user.getId()));
    }

    @Test
    void testGetRoute_UserNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,()->routeService.getRoute(route.getId(),user.getId()));
    }

    @Test
    void testGetRoute_RouteIsNotPublished() {
        route.setIsPublished(false);
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));

        assertThrows(NoResourceFoundException.class,()->routeService.getRoute(route.getId(),user.getId()));
    }

    @Test
    void testGetAllDestinationRoutes_Success() {

        when(destinationRepo.findById(destination.getId())).thenReturn(Optional.of(destination));
        when(routeRepo.findAllByDestination_Id(destination.getId())).thenReturn(Optional.of(List.of(route)));
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ROUTE,route.getId())).thenReturn(List.of(image));
        when(trackPointRepo.findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceDesc(route.getId())).thenReturn(Optional.of(trackPoint));

        ApiResponse<List<RouteDetails>> response = routeService.getAllDestinationRoutes(destination.getId());
        assertNotNull(response);
        assertEquals(200,response.getStatus());

        List<RouteDetails> result = response.getData();
        assertEquals(1,result.size());
        assertEquals(route.getId(),result.get(0).getId());
        assertTrue(result.get(0).getIsPublished());
        assertEquals(destination.getId(),result.get(0).getDestinationId());
    }

    @Test
    void testGetAllDestinationRoutes_IdLessThanOne() {
        destination.setId(0);
        assertThrows(IllegalArgumentException.class,()->routeService.getAllDestinationRoutes(0));
    }

    @Test
    void testGetAllDestinationRoutes_DestinationNotFound(){
        when(destinationRepo.findById(destination.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->routeService.getAllDestinationRoutes(destination.getId()));
    }

    @Test
    void testGetAllDestinationRoutes_RouteNotFound() {
        when(destinationRepo.findById(destination.getId())).thenReturn(Optional.of(destination));
        when(routeRepo.findAllByDestination_Id(destination.getId())).thenReturn(Optional.empty());

        assertThrows(NoResourceFoundException.class,()->routeService.getAllDestinationRoutes(destination.getId()));
    }

    @Test
    void testGetAllRoutes_Success(){
        Page<Route> routePage = new PageImpl<>(Collections.singletonList(route));

        when(routeRepo.count()).thenReturn(1L);
        when(routeRepo.findAll(any(Pageable.class))).thenReturn(routePage);
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ROUTE,route.getId())).thenReturn(List.of(image));
        when(trackPointRepo.findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceDesc(route.getId())).thenReturn(Optional.of(trackPoint));

        ApiResponse<Page<RouteDetails>> response = routeService.getAllRoutes(0,10);
        assertNotNull(response);
        assertEquals(200,response.getStatus());
        assertEquals("routes fetched",response.getMessage());

        Page<RouteDetails> result =  response.getData();
        assertEquals(route.getId(),result.getContent().get(0).getId());
        assertTrue(result.getContent().get(0).getIsPublished());
        assertEquals(route.getDestination().getId(),result.getContent().get(0).getDestinationId());
    }

    @Test
    void testGetAllRoutes_RouteCountLessThanOne(){
        Page<Route> routePage = new PageImpl<>(Collections.singletonList(route));
        when(routeRepo.count()).thenReturn(0L);

        assertThrows(NoResourceFoundException.class,()->routeService.getAllRoutes(0,10));
    }

    @Test
    void testCreateRoute_Success() {
        RouteRequest req = new RouteRequest();
        req.setName("Route 1");
        req.setDestinationId(destination.getId());
        req.setDescription("this is a route description");

        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));
        when(destinationRepo.findById(req.getDestinationId())).thenReturn(Optional.of(destination));
        when(routeRepo.save(any(Route.class))).thenAnswer(i->i.getArgument(0));

        ApiResponse<RouteResponse> response = routeService.createRoute(req,user.getId());

        assertNotNull(response);
        assertEquals(201,response.getStatus());
        assertEquals(req.getName(),response.getData().getName());
        assertEquals(req.getDestinationId(),response.getData().getDestinationId());
    }

    @Test
    void testCreateRoute_UserNotFound(){
        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,()->routeService.createRoute(req,user.getId()));
    }

    @Test
    void testCreateRoute_DestinationNotFound(){
        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));
        when(destinationRepo.findById(req.getDestinationId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->routeService.createRoute(req,user.getId()));
    }

    @Test
    void testCreateRoute_RouteNotSaved(){

        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));
        when(destinationRepo.findById(req.getDestinationId())).thenReturn(Optional.of(destination));
        when(routeRepo.existsByNameAndDestination_Id(req.getName(),destination.getId())).thenReturn(true);
        when(routeRepo.save(any(Route.class))).thenThrow(new DataAccessException("error during db") {
        });

        assertThrows(CreateRouteFailedException.class,()->routeService.createRoute(req,user.getId()));
    }

    @Test
    void testGetRouteGeoJson_Success(){

        route.setRouteStatus(RouteStatus.MERGED);
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));
        GeoJsonFeatureCollection expectedResponse = GeoJsonFeatureCollection.builder().type("FeatureCollection").features(new ArrayList<>()).build();

        when(geoJsonMapper.toGeoJson(route,0.0001)).thenReturn(expectedResponse);

        GeoJsonFeatureCollection response = routeService.getRouteGeoJson(route.getId(),0.0001,user.getId());

        assertNotNull(response);
        assertEquals("FeatureCollection",response.getType());

    }

    @Test
    void testGetRouteGeoJson_RouteNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->routeService.getRouteGeoJson(route.getId(),0.0001,user.getId()));
    }

    @Test
    void testGetRouteGeoJson_UserNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class,()->routeService.getRouteGeoJson(route.getId(),0.0001,user.getId()));
    }

    @Test
    void testGetRouteGeoJson_RouteIsNotPublished(){
        route.setIsPublished(false);
        user.setRole(Role.CUSTOMER);
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));

        assertThrows(NoResourceFoundException.class,()->routeService.getRouteGeoJson(route.getId(),0.0001,user.getId()));
    }

    @Test
    void testGetRouteGeoJson_IdleStatus(){
        route.setRouteStatus(RouteStatus.IDLE);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));

        assertThrows(ResourceNotFoundException.class,()->routeService.getRouteGeoJson(route.getId(),0.0001,user.getId()));

    }

    @Test
    void testGetRouteGeoJson_MergingStatus(){

        route.setRouteStatus(RouteStatus.MERGING);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));

        assertThrows(ResourceNotFoundException.class,()->routeService.getRouteGeoJson(route.getId(),0.0001,user.getId()));

    }

    @Test
    void testGetRouteGeoJson_FailedStatus(){
        route.setRouteStatus(RouteStatus.FAILED);

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));

        assertThrows(ResourceNotFoundException.class,()->routeService.getRouteGeoJson(route.getId(),0.0001,user.getId()));


    }

    @Test
    void testGetRouteGeoJson_RouteCreationFailed(){
        route.setPath(null);
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));
        assertThrows(CreateRouteFailedException.class,()->routeService.getRouteGeoJson(route.getId(),0.0001,user.getId()));
    }

    @Test
    void testUpdateRoute_Success(){

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));
        when(destinationRepo.findById(req.getDestinationId())).thenReturn(Optional.of(destination));
        when(trackPointRepo.findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceAsc(route.getId())).thenReturn(Optional.of(trackPoint));
        when(trackPointRepo.findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceDesc(route.getId())).thenReturn(Optional.of(trackPoint));
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ROUTE,route.getId())).thenReturn(List.of(image));
        when(routeRepo.save(any(Route.class))).thenAnswer(i->i.getArgument(0));

        ApiResponse<RouteResponse> response = routeService.updateRoute(route.getId(),req,user.getId());
        assertNotNull(response);
        assertEquals(route.getMaxElevation(),response.getData().getMaxElevation());
        assertEquals(req.getDifficultyLevel(),response.getData().getDifficultyLevel());
    }

    @Test
    void testUpdateRoute_RouteNotFound(){
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->routeService.updateRoute(route.getId(),req,user.getId()));
    }

    @Test
    void testUpdateRoute_DestinationNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(destinationRepo.findById(req.getDestinationId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->routeService.updateRoute(route.getId(),req,user.getId()));
    }

    @Test
    void testUpdateRoute_UserNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(destinationRepo.findById(req.getDestinationId())).thenReturn(Optional.of(destination));
        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->routeService.updateRoute(route.getId(),req,user.getId()));
    }

    @Test
    void testDeleteRoute_Success(){

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ROUTE,route.getId())).thenReturn(List.of(image));
        when(gpxSegmentRepo.findByRoute(route)).thenReturn(Optional.of(List.of(new GpxSegment())));

        ApiResponse<Void> response = routeService.deleteRoute(route.getId());
        assertNotNull(response);
        assertEquals(200,response.getStatus());
        verify(imageService).deleteImage(image.getId());
    }

    @Test
    void testDeleteRoute_RouteNotFound() {
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->routeService.deleteRoute(route.getId()));
    }

    @Test
    void testDeleteRoute_RouteIsMerging() {
        route.setRouteStatus(RouteStatus.MERGING);
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        assertThrows(ResourceDeletionFailedException.class,()->routeService.deleteRoute(route.getId()));
    }

    @Test
    void testDeleteImagesForRoute_Success() {
        routeService.deleteImagesForRoute(image);
        verify(imageService).deleteImage(image.getId());

    }

    @Test
    void testDeleteImagesForRoute_EmptyImage() {
        doThrow(new RuntimeException("Deletion of image failed")).when(imageService).deleteImage(image.getId());
        assertDoesNotThrow(()->routeService.deleteImagesForRoute(image));
    }

    @Test
    void testGetNearbyRoutes_Success()  {

        when(nearbyRouteProjection.getId()).thenReturn(route.getId());
        when(routeRepo.findNearbyRoutes(nearbyRequest.getLatitude(),nearbyRequest.getLongitude(),nearbyRequest.getRadiusMeters(), nearbyRequest.getLimit())).thenReturn(Collections.singletonList(nearbyRouteProjection));
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(trackPointRepo.findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceDesc(route.getId())).thenReturn(Optional.of(trackPoint));
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ROUTE,route.getId())).thenReturn(List.of(image));

        ApiResponse<List<RouteDetails>> response = routeService.getNearbyRoutes(nearbyRequest);
        assertNotNull(response);
        assertEquals(200,response.getStatus());
        assertEquals(1,response.getData().size());
        assertEquals(route.getName(),response.getData().get(0).getName());
    }

    @Test
    void testGetNearByRoutes_EmptyNearByRoute() {
        when(routeRepo.findNearbyRoutes(nearbyRequest.getLatitude(),nearbyRequest.getLongitude(),nearbyRequest.getRadiusMeters(),nearbyRequest.getLimit())).thenReturn(Collections.emptyList());

        assertThrows(NoResourceFoundException.class,()->routeService.getNearbyRoutes(nearbyRequest));
    }

    @Test
    void testGetRecentlyViewedRoutes_Success() {

        Page<RecentlyViewed> recentPage = new PageImpl<>(Collections.singletonList(recentlyViewed));

        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));
        when(recentlyViewedRepository.findByUser_IdOrderByUpdatedAtDesc(any(Integer.class),any(Pageable.class))).thenReturn(recentPage);
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ROUTE,route.getId())).thenReturn(List.of(image));
        when(trackPointRepo.findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceDesc(route.getId())).thenReturn(Optional.of(trackPoint));

        ApiResponse<Page<RouteDetails>> response = routeService.getRecentlyViewedRoutes(user.getId(),0,10);

        assertNotNull(response);
        assertEquals(200,response.getStatus());

        Page<RouteDetails> routePage = response.getData();
        assertEquals(route.getId(),routePage.getContent().get(0).getId());
        assertTrue(routePage.getContent().get(0).getIsPublished());
    }

    @Test
    void testGetRecentlyViewedRoutes_UserNotFound() {

        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->routeService.getRecentlyViewedRoutes(user.getId(),0,10));
    }

    @Test
    void testGetRecentlyViewedRoutes_EmptyViewedRoutes() {

        Page<RecentlyViewed> recentPage = new PageImpl<>(Collections.emptyList());

        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));
        when(recentlyViewedRepository.findByUser_IdOrderByUpdatedAtDesc(any(Integer.class),any(Pageable.class))).thenReturn(recentPage);

        assertThrows(NoResourceFoundException.class,()->routeService.getRecentlyViewedRoutes(user.getId(),0,10));
    }

    @Test
    void testGetPopularRoutes_Success() {

        Page<Route> routePage = new PageImpl<>(Collections.singletonList(route));

        when(recentlyViewedRepository.findMostPopularRoutes(any(Pageable.class))).thenReturn(routePage);
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ROUTE,route.getId())).thenReturn(List.of(image));
        when(trackPointRepo.findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceDesc(route.getId())).thenReturn(Optional.of(trackPoint));

        ApiResponse<Page<RouteDetails>> response = routeService.getPopularRoutes(0,10);
        assertNotNull(response);
        assertEquals(200,response.getStatus());

        Page<RouteDetails> routeDetails = response.getData();
        assertEquals(route.getId(),routeDetails.getContent().get(0).getId());
        assertEquals(route.getName(),routeDetails.getContent().get(0).getName());
        assertEquals(route.getDistanceInKm(),routeDetails.getContent().get(0).getTotalDistanceInKm());
    }

    @Test
    void testGetPopularRoutes_EmptyPopularRoutes() {

        Page<Route> routePage = new PageImpl<>(Collections.emptyList());

        when(recentlyViewedRepository.findMostPopularRoutes(any(Pageable.class))).thenReturn(routePage);
        assertThrows(NoResourceFoundException.class,()->routeService.getPopularRoutes(0,10));
    }

    @Test
    void testUpdateRecentlyViewedStatus_Success() {

        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(recentlyViewedRepository.findByUser_IdAndRoute_Id(any(Integer.class),eq(route.getId()))).thenReturn(Optional.of(recentlyViewed));

        routeService.updateRecentlyViewedStatus(user.getId(),route.getId());

        ArgumentCaptor<RecentlyViewed> captor = ArgumentCaptor.forClass(RecentlyViewed.class);
        verify(recentlyViewedRepository).save(captor.capture());
        assertEquals(6,captor.getValue().getCounter());
    }

    @Test
    void testUpdateRecentlyViewedStatus_NewRecordSuccess() {

        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));
        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(recentlyViewedRepository.findByUser_IdAndRoute_Id(any(Integer.class),eq(route.getId()))).thenReturn(Optional.empty());

        routeService.updateRecentlyViewedStatus(user.getId(),route.getId());

        ArgumentCaptor<RecentlyViewed> captor = ArgumentCaptor.forClass(RecentlyViewed.class);
        verify(recentlyViewedRepository).save(captor.capture());
        assertEquals(1,captor.getValue().getCounter());
    }

    @Test
    void testUpdateRecentlyViewedStatus_UserNotFound() {

        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->routeService.updateRecentlyViewedStatus(user.getId(),route.getId()));
    }

    @Test
    void testUpdateRecentlyViewedStatus_RouteNotFound() {
        when(userRepo.findById(any(Integer.class))).thenReturn(Optional.of(user));
        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->routeService.updateRecentlyViewedStatus(user.getId(),route.getId()));
    }

    @Test
    void testSearchRoutesByKeyword_Success() {

        Page<Route> routePage = new PageImpl<>(Collections.singletonList(route));

        when(routeRepo.searchByKeyword(any(String.class),any(Pageable.class))).thenReturn(routePage);
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ROUTE,route.getId())).thenReturn(List.of(image));
        when(trackPointRepo.findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceDesc(route.getId())).thenReturn(Optional.of(trackPoint));

        ApiResponse<Page<RouteDetails>> response = routeService.searchRoutesByKeyword("keyword",0,10);
        assertNotNull(response);
        assertEquals(200,response.getStatus());

        Page<RouteDetails> routeDetails = response.getData();
        assertEquals(route.getId(),routeDetails.getContent().get(0).getId());
        assertEquals(route.getName(),routeDetails.getContent().get(0).getName());
    }

    @Test
    void testSearchRoutesByKeyword_EmptyRoute() {
        Page<Route> routePage = new PageImpl<>(Collections.emptyList());
        when(routeRepo.searchByKeyword(anyString(),any(Pageable.class))).thenReturn(routePage);

        assertThrows(ResourceNotFoundException.class,()->routeService.searchRoutesByKeyword("keyword",0,10));
    }

    @Test
    void testGetRecentlyAddedRoutes_Success() {
        Page<Route> routePage = new PageImpl<>(Collections.singletonList(route));

        when(routeRepo.findAllByOrderByTimeStampDesc(any(Pageable.class))).thenReturn(routePage);
        when(imageRepo.findByEntityTypeAndEntityId(EntityType.ROUTE,route.getId())).thenReturn(List.of(image));
        when(trackPointRepo.findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceDesc(route.getId())).thenReturn(Optional.of(trackPoint));

        ApiResponse<Page<RouteDetails>> response = routeService.getRecentlyAddedRoutes(0,10);
        assertNotNull(response);
        assertEquals(200,response.getStatus());

        Page<RouteDetails> resultPage = response.getData();
        assertEquals(route.getId(),resultPage.getContent().get(0).getId());
        assertEquals(route.getDestination().getId(),resultPage.getContent().get(0).getDestinationId());
    }

    @Test
    void testGetRecentlyAddedRoutes_EmptyFoundRoutes() {
        Page<Route> routePage = new PageImpl<>(Collections.emptyList());

        when(routeRepo.findAllByOrderByTimeStampDesc(any(Pageable.class))).thenReturn(routePage);
        assertThrows(NoResourceFoundException.class,()->routeService.getRecentlyAddedRoutes(0,10));

    }

    @Test
    void testUpdateRoutePublishStatus_Success() {


        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(routeRepo.save(any(Route.class))).thenReturn(route);

        ApiResponse<Void> response = routeService.updateRoutePublishStatus(route.getId(),true);
        assertNotNull(response);
        assertEquals(200,response.getStatus());
        assertEquals("route published ", response.getMessage());
    }

    @Test
    void testUpdateRoutePublishStatus_RouteNotFound() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->routeService.updateRoutePublishStatus(route.getId(),true));
    }

    @Test
    void testUpdateRoutePublishStatus_NotSaved() {

        when(routeRepo.findById(route.getId())).thenReturn(Optional.of(route));
        when(routeRepo.save(any(Route.class))).thenThrow(new DataAccessException("failed to save") {
        });

        assertThrows(RuntimeException.class,()->routeService.updateRoutePublishStatus(route.getId(),true));


    }


}
