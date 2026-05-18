package com.example.trekking_app.repository;

import com.example.trekking_app.entity.Route;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface RouteRepository extends JpaRepository<Route,Integer> {

Optional<Route> findById(int routeId);
Optional<Route> findByName(int routeName);
Optional<List<Route>> findByUserId(int userId);
Optional<List<Route>> findAllByDestination_Id(int destinationId);
Boolean existsByName(Integer routeName);
Page<Route> findAll(@NonNull Pageable pageable);
long count();
}
