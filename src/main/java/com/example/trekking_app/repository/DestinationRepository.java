package com.example.trekking_app.repository;

import com.example.trekking_app.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinationRepository extends JpaRepository<Destination , Integer> {

    boolean existsByNameAndDistrictAndRegion(String name,String district,String region);
}
