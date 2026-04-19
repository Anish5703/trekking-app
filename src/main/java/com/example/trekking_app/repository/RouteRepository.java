package com.example.trekking_app.repository;

import com.example.trekking_app.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route,Integer> {

Optional<Route> findById(int routeId);
Optional<Route> findByTitle(int routeTitle);
Optional<List<Route>> findByUserId(int userId);
Boolean existsByTitle(int routeTitle);
long count();
}
