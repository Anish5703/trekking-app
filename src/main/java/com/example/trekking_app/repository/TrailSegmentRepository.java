package com.example.trekking_app.repository;

import com.example.trekking_app.entity.TrailSegment;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface TrailSegmentRepository extends JpaRepository<TrailSegment,Integer> {


    Page<TrailSegment> findByRoute_Id(Integer routeId, Pageable pageable);
    List<TrailSegment> findByRoute_Id(Integer routeId);

    void deleteByGpxSegment_Id(int gpxSegment_id);

    Optional<TrailSegment> findByIdAndRoute_Id(@NonNull Integer trailSegmentId, @NonNull Integer routeId);
}
