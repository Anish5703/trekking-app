package com.example.trekking_app.repository;

import com.example.trekking_app.entity.POI;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface POIRepository extends JpaRepository<POI,Integer> {


    Integer deleteAllByWayPoint_GpxSegment_Id(Integer gpxSegmentId);

    List<POI> findByRoute_id(Integer routeId);

    Page<POI> findByRoute_id(Integer routeId, Pageable pageable);

    Optional<POI> findByIdAndRouteId(int routeId, int poiId);
}
