package com.example.trekking_app.repository;

import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackPointRepository extends JpaRepository<TrackPoint,Integer> {


     void deleteAllByRoute_Id(int routeId);
     void deleteAllByGpxSegment_Id(int routeId);
     boolean existsByRoute_Id(int routeId);
     List<TrackPoint>findByRoute_IdAndGpxSegment_IdOrderByLocalSequenceAsc(Integer routeId,Integer gpxSegmentId);

     Optional<List<TrackPoint>> findByRouteAndIsDeletedFalseOrderByGlobalSequenceAsc(Route route);

     Page<TrackPoint>findByRoute_IdAndGpxSegment_IdOrderByLocalSequenceAsc(Integer routeId, Integer gpxSegmentId, Pageable pageable);
     Page<TrackPoint> findByRoute_IdAndIsDeletedFalseOrderByGlobalSequenceAsc(Integer routeId,Pageable pageable);
     Page<TrackPoint> findByRoute_IdOrderByGlobalSequenceAsc(Integer routeId,Pageable pageable);

     Optional<TrackPoint> findByIdAndRoute_Id(Integer trackPointId, Integer routeId);

    Page<TrackPoint> findByRoute_IdAndIsDeletedTrueOrderByUpdateAtAsc(@NonNull Integer routeId, Pageable pageable);
}
