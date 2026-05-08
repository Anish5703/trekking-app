package com.example.trekking_app.repository;

import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GpxSegmentRepository extends JpaRepository<GpxSegment,Integer> {

    List<GpxSegment> findByRoute_IdOrderByOrderIndexAsc(Integer routeId);
    Optional<List<GpxSegment>> findByRoute(Route route);

    Optional<GpxSegment> findByIdAndRoute_Id(@NonNull Integer routeId, @NonNull Integer gpxSegmentId);
}
