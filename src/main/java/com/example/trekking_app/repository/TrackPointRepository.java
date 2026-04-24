package com.example.trekking_app.repository;

import com.example.trekking_app.entity.TrackPoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackPointRepository extends JpaRepository<TrackPoint,Integer> {


     void deleteAllByRoute_Id(int routeId);
     boolean existsByRoute_Id(int routeId);
}
