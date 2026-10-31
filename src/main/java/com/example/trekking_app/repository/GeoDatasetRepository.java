package com.example.trekking_app.repository;
import com.example.trekking_app.entity.GeoDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface GeoDatasetRepository extends JpaRepository<GeoDataset, Long> {
    Optional<GeoDataset> findByDatasetKey(String datasetKey);
    List<GeoDataset> findByActiveTrue();
}
