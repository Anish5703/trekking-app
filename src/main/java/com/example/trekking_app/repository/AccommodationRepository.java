package com.example.trekking_app.repository;

import com.example.trekking_app.dto.accommodation.NearbyAccommodationProjection;
import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.Route;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccommodationRepository extends JpaRepository<Accommodation,Integer> {
    boolean existsByRoute_IdAndLongitudeAndLatitude(Route route, @NonNull Double longitude, @NonNull Double latitude);

    void deleteByWayPoint_GpxSegment_Id(int gpxSegmentId);

    Page<Accommodation> findByRoute_Id(Integer routeId, Pageable pageable);

    @Query(value = """
    SELECT
        p.id,
        p.name,
        p.description,
        p.type,
        p.latitude,
        p.longitude,
        p.elevation,
        p.route_id,
        ST_Distance(
            ST_SetSRID(ST_MakePoint(p.longitude, p.latitude), 4326)::geography,
            ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
        ) AS distance_meters
    FROM accommodations acc
        JOIN pois p ON p.id = acc.id
        LEFT JOIN routes r ON r.id = p.route_id
    WHERE ST_DWithin(
        ST_SetSRID(ST_MakePoint(p.longitude, p.latitude), 4326)::geography,
        ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
        :radiusMeters
    )
    ORDER BY distance_meters ASC
    LIMIT :limit
    """, nativeQuery = true)
    List<NearbyAccommodationProjection> findNearbyAccommodation(
            @Param("lon") double longitude,
            @Param("lat") double latitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("limit") int limit
    );
}
