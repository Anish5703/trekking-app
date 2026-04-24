package com.example.trekking_app.entity;

import com.example.trekking_app.model.AlertSeverity;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Geometry;

@Entity
@Table(name = "danger_zones" , indexes = {
        @Index(name = "idx_danger_zone_route" , columnList = "route_id") ,
        @Index(name = "idx_danger_zone_severity" , columnList = "severity") ,
        @Index(name = "idx_danger_zone_geom" , columnList = "zone_geometry")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DangerZone extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id",nullable = false)
    private Route route;

    @Column(nullable = false,length = 50)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private AlertSeverity severity;

    @Column(name = "center_latitude" , nullable = false)
    private Double centerLatitude;

    @Column(name = "center_longitude" , nullable = false)
    private Double centerLongitude;

    @Column(name = "radius_meter")
    @Builder.Default
    private Double radiusMeter = 500.0;

    @Column(name = "zone_geometry" ,columnDefinition = "geometry(Geometry , 4326)")
  private Geometry geom;


}
