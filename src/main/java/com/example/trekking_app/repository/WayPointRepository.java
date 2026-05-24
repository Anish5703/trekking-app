package com.example.trekking_app.repository;

import com.example.trekking_app.entity.WayPoint;
import com.example.trekking_app.model.GpxSegmentStatus;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface WayPointRepository extends JpaRepository<WayPoint,Integer> {


    Optional<WayPoint> findByGpxSegment_IdAndLocalSequence(Integer gpxSegmentId, Integer wpNum);

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE way_points wp
    SET global_sequence = sub.new_seq
    FROM (
        SELECT wp2.id,
               ROW_NUMBER() OVER (
                   ORDER BY gs.order_index, wp2.local_sequence
               ) AS new_seq
        FROM way_points wp2
        JOIN gpx_segments gs ON wp2.gpx_segment_id = gs.id
        WHERE wp2.route_id = :routeId
            AND gs.segment_status = 'WAYPOINT'
    ) sub
    WHERE wp.id = sub.id
    """, nativeQuery = true)
    void updateGlobalSequences(@Param("routeId") Integer routeId);

    // In repository
    @Query("""
    SELECT MIN(wp.elevation)
    FROM WayPoint wp
        JOIN wp.gpxSegment gs
    WHERE wp.route.id = :routeId
      AND gs.segmentStatus = 'WAYPOINT'
      AND wp.isDeleted = false
      AND wp.elevation IS NOT NULL
    """)
    Optional<Double> findMinElevation(@Param("routeId") Integer routeId);

    @Query("""
    SELECT MAX(wp.elevation)
    FROM WayPoint wp
        JOIN wp.gpxSegment gs
    WHERE wp.route.id = :routeId
      AND gs.segmentStatus = 'WAYPOINT'
      AND wp.isDeleted = false
      AND wp.elevation IS NOT NULL
    """)
    Optional<Double> findMaxElevation(@Param("routeId") Integer routeId);

    void deleteAllByGpxSegment_Id(Integer id);

    Page<WayPoint> findByRoute_IdOrderByGlobalSequenceAsc(@NonNull Integer routeId, Pageable pageable);

    Page<WayPoint> findByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceAsc(Integer routeId, Pageable pageable);

    Page<WayPoint> findByRoute_IdAndIsDeletedTrueOrderByUpdatedAtAsc(Integer routeId, Pageable pageable);

    Optional<WayPoint> findByIdAndRoute_Id(@NonNull Integer wayPointId, Integer id);
}
