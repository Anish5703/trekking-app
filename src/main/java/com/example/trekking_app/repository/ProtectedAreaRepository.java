package com.example.trekking_app.repository;
import com.example.trekking_app.entity.ProtectedArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface ProtectedAreaRepository extends JpaRepository<ProtectedArea, Long> {
    @Modifying @Query("delete from ProtectedArea p where p.dataset.id = :datasetId") int deleteForDataset(@Param("datasetId") Long datasetId);
}
