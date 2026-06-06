package com.example.trekking_app.repository;

import com.example.trekking_app.entity.RecentlyViewed;
import com.example.trekking_app.entity.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RecentlyViewedRepository extends JpaRepository<RecentlyViewed,Integer> {

    Page<RecentlyViewed> findByUser_IdOrderByUpdatedAtDesc(int userId, Pageable pageable);
    @Query("SELECT rv.route FROM RecentlyViewed rv GROUP BY rv.route ORDER BY SUM(rv.counter) DESC")
    Page<Route> findMostPopularRoutes(Pageable pageable);

    Optional<RecentlyViewed> findByUser_IdAndRoute_Id(int userId, int route_id);
}
