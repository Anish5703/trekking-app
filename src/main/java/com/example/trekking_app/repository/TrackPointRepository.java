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
import org.springframework.transaction.annotation.Transactional;

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

    // Find the trackpoint just before the waypoint coordinates (by globalSequence)
    @Query("""
    SELECT t FROM TrackPoint t
    WHERE t.route.id = :routeId
      AND t.gpxSegment.segmentStatus = com.example.trekking_app.model.GpxSegmentStatus.TRACKPOINT
      AND t.globalSequence = (
            SELECT MAX(t2.globalSequence)
            FROM TrackPoint t2
            WHERE t2.route.id = :routeId
              AND t2.gpxSegment.segmentStatus = com.example.trekking_app.model.GpxSegmentStatus.TRACKPOINT
              AND t2.globalSequence < (
                    SELECT t3.globalSequence
                    FROM TrackPoint t3
                    WHERE t3.route.id = :routeId
                      AND t3.gpxSegment.segmentStatus = com.example.trekking_app.model.GpxSegmentStatus.TRACKPOINT
                    ORDER BY (
                        ABS(t3.latitude - :lat) +
                        ABS(t3.longitude - :lon)
                    ) ASC
                    LIMIT 1
              )
      )
""")
    Optional<TrackPoint> findPredecessorByCoordinates(
            @Param("routeId") Integer routeId,
            @Param("lat") double lat,
            @Param("lon") double lon
    );



    @Modifying
    @Query("UPDATE TrackPoint t SET t.localSequence = t.localSequence + 1 " +
            "WHERE t.gpxSegment.id = :segmentId AND t.localSequence > :afterSequence")
    void shiftLocalSequencesAfter(@Param("segmentId") Integer segmentId,
                                  @Param("afterSequence") int afterSequence);

    boolean existsByLatitudeAndLongitude(double latitude, double longitude);

    Page<TrackPoint> findByRoute_IdAndIsDeletedTrueOrderByUpdatedAtAsc(@NonNull Integer routeId, Pageable pageable);
    @Query(value = """
    SELECT * FROM track_points
    WHERE route_id = :routeId
      AND is_deleted = false
    ORDER BY ST_Distance(geom, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326))
    LIMIT 1
    """, nativeQuery = true)
    Optional<TrackPoint> findNearestToCoordinatesInRoute(
            @Param("routeId") Integer routeId,
            @Param("lat") Double lat,
            @Param("lon") Double lon
    );

    @Query("""
    SELECT t FROM TrackPoint t
    WHERE t.route.id = :routeId
      AND t.isDeleted = false
      AND t.globalSequence BETWEEN :startSeq AND :endSeq
    ORDER BY t.globalSequence ASC
    """)
    List<TrackPoint> findBetweenGlobalSequences(
            @Param("routeId") Integer routeId,
            @Param("startSeq") Integer startSeq,
            @Param("endSeq") Integer endSeq
    );

     // Replaces the entire for-loop
     @Modifying(
             flushAutomatically = true,
             clearAutomatically = true
     )
     @Transactional
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
            AND gs.segment_status = 'TRACKPOINT'
    ) sub
    WHERE tp.id = sub.id
    """, nativeQuery = true)
     void updateGlobalSequences(@Param("routeId") Integer routeId);

     // In repository
     @Query("""
    SELECT MIN(tp.elevation)
    FROM TrackPoint tp
    JOIN tp.gpxSegment gs
           WHERE tp.route.id = :routeId
      AND gs.segmentStatus = 'TRACKPOINT'
      AND tp.isDeleted = false
      AND tp.elevation IS NOT NULL
   """)
     Optional<Double> findMinElevation(@Param("routeId") Integer routeId);

     @Query("""
    SELECT MAX(tp.elevation)
    FROM TrackPoint tp
    JOIN tp.gpxSegment gs
    WHERE tp.route.id = :routeId
      AND gs.segmentStatus = 'TRACKPOINT'
      AND tp.isDeleted = false
      AND tp.elevation IS NOT NULL
    """)
     Optional<Double> findMaxElevation(@Param("routeId") Integer routeId);

    Optional<TrackPoint> findFirstByLatitudeAndLongitude(Double latitude, Double longitude);

    Optional<TrackPoint> findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceAsc(Integer routeId);
    Optional<TrackPoint> findFirstByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceDesc(Integer routeId);

    Optional<TrackPoint> findByRoute_IdAndLatitudeAndLongitude(Integer routeId, Double latitude, Double longitude);
}

