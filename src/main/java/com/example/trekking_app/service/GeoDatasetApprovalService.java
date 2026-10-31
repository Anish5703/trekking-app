package com.example.trekking_app.service;
import com.example.trekking_app.entity.GeoDataset;
import com.example.trekking_app.repository.GeoDatasetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class GeoDatasetApprovalService {
    private final GeoDatasetRepository repository;
    public GeoDatasetApprovalService(GeoDatasetRepository repository) { this.repository=repository; }
    @Transactional
    public void setRedistributionApproval(String key, boolean allowed, String reference) {
        GeoDataset dataset = repository.findByDatasetKey(key).orElseThrow(() -> new GeoOfflineException(404,"GEO_DATASET_NOT_FOUND","Dataset was not found."));
        if (allowed) dataset.approveRedistribution(reference); else dataset.revokeRedistribution();
    }
}
