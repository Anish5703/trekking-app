package com.example.trekking_app.repository;

import com.example.trekking_app.entity.TrackPoint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackPointRepository extends JpaRepository<TrackPoint,Integer> {

    void deleteByRouteId(int routeId);
}
