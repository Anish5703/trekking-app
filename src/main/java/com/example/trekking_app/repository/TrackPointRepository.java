package com.example.trekking_app.repository;

import com.example.trekking_app.entity.Route;
import com.example.trekking_app.entity.TrackPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackPointRepository extends JpaRepository<TrackPoint,Integer> {


     void deleteAllByRoute_Id(int routeId);
     void deleteAllByGpxSegment_Id(int routeId);
     boolean existsByRoute_Id(int routeId);

     Optional<List<TrackPoint>> findByRouteAndIsDeletedFalseOrderByGlobalSequenceAsc(Route route);
}
