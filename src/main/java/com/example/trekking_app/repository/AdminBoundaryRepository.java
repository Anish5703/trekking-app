package com.example.trekking_app.repository;
import com.example.trekking_app.entity.AdminBoundary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
public interface AdminBoundaryRepository extends JpaRepository<AdminBoundary, Long> {
    @Modifying @Query("delete from AdminBoundary b where b.dataset.id = :datasetId") int deleteForDataset(@Param("datasetId") Long datasetId);
}
