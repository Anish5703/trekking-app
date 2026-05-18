package com.example.trekking_app.repository;

import com.example.trekking_app.entity.POI;
import org.springframework.data.jpa.repository.JpaRepository;

public interface POIRepository extends JpaRepository<POI,Integer> {


    Integer deleteAllByWayPoint_GpxSegment_Id(Integer gpxSegmentId);
}
