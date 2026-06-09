package com.example.trekking_app.repository;

import com.example.trekking_app.dto.poi.NearbyPoiProjection;
import com.example.trekking_app.entity.POI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface POIRepository extends JpaRepository<POI,Integer> {


    Integer deleteAllByWayPoint_GpxSegment_Id(Integer gpxSegmentId);

    List<POI> findByRoute_id(Integer routeId);

    Page<POI> findByRoute_id(Integer routeId, Pageable pageable);

    Optional<POI> findByIdAndRouteId(int routeId, int poiId);

    @Query(value = """
    SELECT
        p.id,
        p.name,
        p.description,
        p.type,
        p.latitude,
        p.longitude,
        p.elevation,
        r.id AS route_id,
        ST_Distance(
            ST_SetSRID(ST_MakePoint(p.longitude, p.latitude), 4326)::geography,
            ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
        ) AS distance_meters
    FROM pois p
        LEFT JOIN routes r ON r.id = p.route_id
    WHERE ST_DWithin(
        ST_SetSRID(ST_MakePoint(p.longitude, p.latitude), 4326)::geography,
        ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
        :radiusMeters
    )
    ORDER BY distance_meters ASC
    LIMIT :limit
    """, nativeQuery = true)
    List<NearbyPoiProjection> findNearbyPois(
            @Param("lon") double longitude,
            @Param("lat") double latitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("limit") int limit
    );
}
