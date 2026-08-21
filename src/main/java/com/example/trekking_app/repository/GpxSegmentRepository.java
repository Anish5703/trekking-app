package com.example.trekking_app.repository;

import com.example.trekking_app.entity.GpxSegment;
import com.example.trekking_app.entity.Route;
import com.example.trekking_app.model.GpxSegmentStatus;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GpxSegmentRepository extends JpaRepository<GpxSegment,Integer> {

    List<GpxSegment> findByRoute_IdOrderByOrderIndexAsc(Integer routeId);
    Optional<List<GpxSegment>> findByRoute(Route route);
    Optional<GpxSegment> findByIdAndRoute_Id(@NonNull Integer segmentId, @NonNull Integer routeId);

    Optional<GpxSegment> findTopByRoute_IdAndSegmentStatusOrderByOrderIndexDesc(@NonNull Integer routeId, GpxSegmentStatus segmentStatus);

    Optional<List<GpxSegment>> findByRouteAndSegmentStatus(Route route, GpxSegmentStatus segmentStatus);

    Optional<GpxSegment> findByRoute_IdAndSegmentStatusAndOrderIndex(Integer id, GpxSegmentStatus gpxSegmentStatus, Integer gpxOrderIndex);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE GpxSegment g SET g.orderIndex = g.orderIndex + 1 " +
            "WHERE g.route.id = :routeId AND g.orderIndex > :afterOrderIndex")
    void shiftOrderIndexAfter(@Param("routeId") Integer routeId, @Param("afterOrderIndex") int afterOrderIndex);
}
