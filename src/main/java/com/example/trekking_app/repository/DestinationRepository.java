package com.example.trekking_app.repository;

import com.example.trekking_app.entity.Destination;
import com.example.trekking_app.entity.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DestinationRepository extends JpaRepository<Destination , Integer> {

    boolean existsByNameAndDistrictAndRegion(String name,String district,String region);

    @Query("SELECT d FROM Destination d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(d.district) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(d.region) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Destination> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
