package com.example.trekking_app.repository;

import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.model.GpxSegmentStatus;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GpxSegmentRepository extends JpaRepository<GpxSegment,Integer> {

    List<GpxSegment> findByRoute_IdOrderByOrderIndexAsc(Integer routeId);
    Optional<List<GpxSegment>> findByRoute(Route route);
    Optional<GpxSegment> findByIdAndRoute_Id(@NonNull Integer segmentId, @NonNull Integer routeId);

    Optional<GpxSegment> findByRoute_IdAndSegmentStatusOrderByOrderIndexAsc(@NonNull Integer routeId, GpxSegmentStatus segmentStatus);

    Optional<List<GpxSegment>> findByRouteAndSegmentStatus(Route route, GpxSegmentStatus segmentStatus);



    Optional<GpxSegment> findByRoute_IdAndSegmentStatusAndOrderIndex(Integer id, GpxSegmentStatus gpxSegmentStatus, Integer gpxOrderIndex);
}
