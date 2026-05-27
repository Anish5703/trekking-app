package com.example.trekking_app.repository;

import com.example.trekking_app.entity.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccommodationRepository extends JpaRepository<Accommodation,Integer> {
}
