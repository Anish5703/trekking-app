package com.example.trekking_app.repository;

import com.example.trekking_app.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route,Integer> {

Optional<Route> findById(int routeId);
Optional<Route> findByName(int routeName);
Optional<List<Route>> findByUserId(int userId);
Optional<List<Route>> findAllByDestination_Id(int destinationId);
Boolean existsByName(int routeName);
long count();
}
