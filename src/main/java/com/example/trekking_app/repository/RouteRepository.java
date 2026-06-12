package com.example.trekking_app.repository;

import com.example.trekking_app.dto.route.NearbyRouteProjection;
import com.example.trekking_app.dto.route.NearbyRouteResponse;
import com.example.trekking_app.entity.Route;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    boolean existsByNameAndDestination_Id(String name, Integer id);

    @Query(value = """
    SELECT
        r.id,
        r.name,
        r.description,
        r.difficulty_level,
        r.distance_in_km,
        r.estimated_days,
        r.route_status,
        d.name AS destination_name,
        ST_Distance(
            r.path::geography,
            ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
        ) AS distance_meters
    FROM routes r
        JOIN destinations d ON d.id = r.destination_id
    WHERE ST_DWithin(
        r.path::geography,
        ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
        :radiusMeters
    )
    AND r.route_status = 'MERGED'
    ORDER BY distance_meters ASC
    LIMIT :limit
    """, nativeQuery = true)
    List<NearbyRouteProjection> findNearbyRoutes(
            @Param("lon") double longitude,
            @Param("lat") double latitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("limit") int limit
    );
    @Query("SELECT r FROM Route r JOIN r.destination d WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Route> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);


    Page<Route> findAllByOrderByTimeStampDesc(Pageable pageable);}
