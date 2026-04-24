package com.example.trekking_app.service;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.route.RouteRequest;
import com.example.trekking_app.dto.route.RouteResponse;
import com.example.trekking_app.entity.Destination;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.exception.auth.UserNotFoundException;
import com.example.trekking_app.exception.route.CreateRouteFailedException;
import com.example.trekking_app.exception.destination.DestinationNotFoundException;
import com.example.trekking_app.mapper.RouteMapper;
import com.example.trekking_app.repository.DestinationRepository;
import com.example.trekking_app.repository.RouteRepository;
import com.example.trekking_app.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RouteService {

    private final RouteRepository routeRepo;
    private final DestinationRepository destinationRepo;
    private final UserRepository userRepo;
    private final RouteMapper routeMapper;

    public RouteService(RouteRepository routeRepo ,UserRepository userRepo , DestinationRepository destinationRepo)
    {
        this.routeRepo = routeRepo;
        this.destinationRepo = destinationRepo;
        this.userRepo  = userRepo;
        this.routeMapper = new RouteMapper();
    }

    @Transactional
    public ApiResponse<RouteResponse> createRoute(RouteRequest routeRequest,int userId)
    {
      User user = userRepo.findById(userId).orElseThrow(
              () -> new UserNotFoundException("No user found with id "+userId)
      );
        Destination destination = destinationRepo.findById(routeRequest.getDestinationId()).orElseThrow(
                () -> new DestinationNotFoundException("No destination found with id "+routeRequest.getDestinationId())
        );

        Route route = routeMapper.toEntity(routeRequest,user,destination);
      try{
         Route newRoute =  routeRepo.save(route);
         RouteResponse routeResponse = routeMapper.toRouteResponse(newRoute);
         return new ApiResponse<>(routeResponse,"New route created",201);
      }
      catch(Exception ex)
      {
          log.error("Failed to create new route : {}",ex.getLocalizedMessage());
          throw new CreateRouteFailedException("Failed to create new route");
      }
    }
}
