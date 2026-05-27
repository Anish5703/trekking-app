package com.example.trekking_app.repository;

import com.example.trekking_app.entity.TrailSegment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrailSegmentRepository extends JpaRepository<TrailSegment,Integer> {
}
