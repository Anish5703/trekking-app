package com.example.trekking_app.repository;

import com.example.trekking_app.entity.POI;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface POIRepository extends JpaRepository<POI,Integer> {


    Integer deleteAllByWayPoint_GpxSegment_Id(Integer gpxSegmentId);

    List<POI> findByRoute_id(Integer id);

    Optional<POI> findByIdAndRouteId(int routeId, int poiId);
}
