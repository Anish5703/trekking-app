package com.example.trekking_app.entity;

import jakarta.persistence.*;
import org.locationtech.jts.geom.MultiPolygon;
import java.time.Instant;

@Entity
@Table(name = "geo_admin_boundary", uniqueConstraints = @UniqueConstraint(name = "uk_geo_admin_level_source_dataset", columnNames = {"boundary_level", "source_code", "dataset_id"}))
public class AdminBoundary {
    public enum Level { PROVINCE, DISTRICT, MUNICIPALITY }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "boundary_level", nullable = false, length = 24) private Level level;
    @Column(name = "source_code", nullable = false, length = 160) private String sourceCode;
    @Column(name = "source_name", nullable = false, length = 255) private String sourceName;
    @Column(name = "display_name", nullable = false, length = 255) private String displayName;
    @Column(name = "parent_source_code", length = 160) private String parentSourceCode;
    @Column(name = "district_name", length = 255) private String districtName;
    @Column(name = "province_name", length = 255) private String provinceName;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "dataset_id", nullable = false) private GeoDataset dataset;
    @Column(name = "geom", nullable = false, columnDefinition = "geometry(MultiPolygon,4326)") private MultiPolygon geometry;
    @Column(name = "source_attributes", columnDefinition = "text") private String sourceAttributes;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    protected AdminBoundary() {}
    public AdminBoundary(Level level, String code, String sourceName, String displayName, String parentCode,
                         String districtName, String provinceName, GeoDataset dataset, MultiPolygon geometry, String sourceAttributes) {
        this.level=level; this.sourceCode=code; this.sourceName=sourceName; this.displayName=displayName;
        this.parentSourceCode=parentCode; this.districtName=districtName; this.provinceName=provinceName;
        this.dataset=dataset; this.geometry=geometry; this.sourceAttributes=sourceAttributes; this.updatedAt=Instant.now();
    }
    public Long getId(){return id;} public Level getLevel(){return level;} public String getSourceCode(){return sourceCode;}
    public String getSourceName(){return sourceName;} public String getDisplayName(){return displayName;}
    public String getParentSourceCode(){return parentSourceCode;} public String getDistrictName(){return districtName;}
    public String getProvinceName(){return provinceName;} public GeoDataset getDataset(){return dataset;}
    public MultiPolygon getGeometry(){return geometry;} public String getSourceAttributes(){return sourceAttributes;} public Instant getUpdatedAt(){return updatedAt;}
}
