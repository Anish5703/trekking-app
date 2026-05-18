package com.example.trekking_app.repository;

import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackPointRepository extends JpaRepository<TrackPoint,Integer> {


     void deleteAllByRoute_Id(int routeId);
     void deleteAllByGpxSegment_Id(int routeId);
     boolean existsByRoute_Id(int routeId);
     Optional<List<TrackPoint>>findByRoute_IdAndGpxSegment_IdOrderByLocalSequenceAsc(Integer routeId,Integer gpxSegmentId);

     Optional<List<TrackPoint>> findByRouteAndIsDeletedFalseOrderByGlobalSequenceAsc(Route route);

     Page<TrackPoint>findByRoute_IdAndGpxSegment_IdOrderByLocalSequenceAsc(Integer routeId, Integer gpxSegmentId, Pageable pageable);
     Page<TrackPoint> findByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceAsc(Integer routeId,Pageable pageable);
     Page<TrackPoint> findByRoute_IdOrderByGlobalSequenceAsc(Integer routeId,Pageable pageable);

     Optional<TrackPoint> findByIdAndRoute_Id(Integer trackPointId, Integer routeId);

    Page<TrackPoint> findByRoute_IdAndIsDeletedTrueOrderByUpdatedAtAsc(@NonNull Integer routeId, Pageable pageable);

     // Replaces the entire for-loop
     @Modifying
     @Query(value = """
    UPDATE track_points tp
    SET global_sequence = sub.new_seq
    FROM (
        SELECT tp2.id,
               ROW_NUMBER() OVER (
                   ORDER BY gs.order_index, tp2.local_sequence
               ) AS new_seq
        FROM track_points tp2
        JOIN gpx_segments gs ON tp2.gpx_segment_id = gs.id
        WHERE tp2.route_id = :routeId
    ) sub
    WHERE tp.id = sub.id
    """, nativeQuery = true)
     void updateGlobalSequences(@Param("routeId") Integer routeId);

     // In repository
     @Query("""
    SELECT MIN(tp.elevation)
    FROM TrackPoint tp
    WHERE tp.route.id = :routeId
      AND tp.isDeleted = false
      AND tp.elevation IS NOT NULL
    """)
     Optional<Double> findMinElevation(@Param("routeId") Integer routeId);

     @Query("""
    SELECT MAX(tp.elevation)
    FROM TrackPoint tp
    WHERE tp.route.id = :routeId
      AND tp.isDeleted = false
      AND tp.elevation IS NOT NULL
    """)
     Optional<Double> findMaxElevation(@Param("routeId") Integer routeId);
}
