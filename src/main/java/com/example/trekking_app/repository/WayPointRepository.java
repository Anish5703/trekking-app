package com.example.trekking_app.repository;

import com.example.trekking_app.entity.WayPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WayPointRepository extends JpaRepository<WayPoint,Integer> {


    Optional<WayPoint> findByGpxSegment_IdAndLocalSequence(Integer gpxSegmentId, Integer wpNum);
}
