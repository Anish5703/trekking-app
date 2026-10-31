package com.example.trekking_app.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "geo_dataset", uniqueConstraints = @UniqueConstraint(name = "uk_geo_dataset_key", columnNames = "dataset_key"))
public class GeoDataset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "dataset_key", nullable = false, length = 80) private String datasetKey;
    @Column(name = "source_name", nullable = false, length = 255) private String sourceName;
    @Column(name = "source_version", length = 100) private String sourceVersion;
    @Column(name = "license_name", length = 255) private String licenseName;
    @Column(name = "attribution", length = 1000) private String attribution;
    @Column(name = "redistribution_allowed", nullable = false) private boolean redistributionAllowed = false;
    @Column(name = "active", nullable = false) private boolean active = false;
    @Column(name = "imported_at") private Instant importedAt;
    @Column(name = "record_count") private int recordCount;
    @Column(name = "content_sha256", length = 64) private String contentSha256;
    @Column(name = "redistribution_approval_reference", length = 1000) private String redistributionApprovalReference;
    @Column(name = "redistribution_approved_at") private Instant redistributionApprovedAt;

    protected GeoDataset() {}
    public GeoDataset(String key, String source, String version, String license, String attribution) {
        this.datasetKey = key; this.sourceName = source; this.sourceVersion = version;
        this.licenseName = license; this.attribution = attribution;
    }
    public Long getId() { return id; }
    public String getDatasetKey() { return datasetKey; }
    public String getSourceName() { return sourceName; }
    public String getSourceVersion() { return sourceVersion; }
    public String getLicenseName() { return licenseName; }
    public String getAttribution() { return attribution; }
    public boolean isRedistributionAllowed() { return redistributionAllowed; }
    public boolean isActive() { return active; }
    public Instant getImportedAt() { return importedAt; }
    public int getRecordCount() { return recordCount; }
    public String getContentSha256() { return contentSha256; }
    public String getRedistributionApprovalReference() { return redistributionApprovalReference; }
    public Instant getRedistributionApprovedAt() { return redistributionApprovedAt; }
    public void approveRedistribution(String reference) {
        if (reference == null || reference.isBlank()) throw new IllegalArgumentException("Approval reference is required.");
        redistributionAllowed = true; redistributionApprovalReference = reference.trim(); redistributionApprovedAt = Instant.now();
    }
    public void revokeRedistribution() { redistributionAllowed = false; redistributionApprovalReference = null; redistributionApprovedAt = null; }
    public void replaceImport(String version, int count, String sha256) {
        sourceVersion = version; recordCount = count; contentSha256 = sha256;
        importedAt = Instant.now(); active = true;
        // Any import invalidates prior permission approval until a human re-confirms it.
        redistributionAllowed = false; redistributionApprovalReference = null; redistributionApprovedAt = null;
    }
}
