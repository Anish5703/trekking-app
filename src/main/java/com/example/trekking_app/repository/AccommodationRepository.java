package com.example.trekking_app.repository;

import com.example.trekking_app.entity.Accommodation;
import com.example.trekking_app.entity.Route;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccommodationRepository extends JpaRepository<Accommodation,Integer> {
    boolean existsByRoute_IdAndLongitudeAndLatitude(Route route, @NonNull Double longitude, @NonNull Double latitude);
}
