package com.example.trekking_app.entity;

import jakarta.persistence.*;
import org.locationtech.jts.geom.MultiPolygon;
import java.time.Instant;

@Entity
@Table(name = "geo_protected_area", uniqueConstraints = @UniqueConstraint(name = "uk_geo_pa_source_dataset", columnNames = {"source_code", "dataset_id"}))
public class ProtectedArea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "source_code", nullable = false, length = 100) private String sourceCode;
    @Column(name = "source_name", nullable = false, length = 255) private String sourceName;
    @Column(name = "display_name", nullable = false, length = 255) private String displayName;
    @Column(name = "area_type", length = 100) private String areaType;
    @Column(name = "area_sq_km") private Double areaSqKm;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "dataset_id", nullable = false) private GeoDataset dataset;
    @Column(name = "geom", nullable = false, columnDefinition = "geometry(MultiPolygon,4326)") private MultiPolygon geometry;
    @Column(name = "source_attributes", columnDefinition = "text") private String sourceAttributes;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    protected ProtectedArea() {}
    public ProtectedArea(String code, String sourceName, String displayName, String type, Double areaSqKm, GeoDataset dataset, MultiPolygon geometry, String sourceAttributes) {
        this.sourceCode=code; this.sourceName=sourceName; this.displayName=displayName; this.areaType=type;
        this.areaSqKm=areaSqKm; this.dataset=dataset; this.geometry=geometry; this.sourceAttributes=sourceAttributes; this.updatedAt=Instant.now();
    }
    public Long getId(){return id;} public String getSourceCode(){return sourceCode;} public String getSourceName(){return sourceName;}
    public String getDisplayName(){return displayName;} public String getAreaType(){return areaType;} public Double getAreaSqKm(){return areaSqKm;}
    public GeoDataset getDataset(){return dataset;} public MultiPolygon getGeometry(){return geometry;} public String getSourceAttributes(){return sourceAttributes;}
}
