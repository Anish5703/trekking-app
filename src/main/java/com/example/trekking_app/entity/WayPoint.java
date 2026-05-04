package com.example.trekking_app.entity;


import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "way_points" , uniqueConstraints = {
        @UniqueConstraint(name = "uq_wp_segment_local",  columnNames = {"gpx_segment_id", "local_sequence"}),
        @UniqueConstraint(name = "uq_wp_route_global",   columnNames = {"route_id", "global_sequence"}),

},
        indexes = {
                @Index(name = "idx_wp_route_number", columnList = "route_id, waypoint_number")})

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class WayPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="route_id")
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="gpx_segment_id")
    private GpxSegment gpxSegment;

    @Column(nullable = false , length = 100)
    private String name;

    /** XLSX join key — string to preserve leading zeros (e.g. "001"). */
    @Column(name = "waypoint_number", length = 32)
    private String waypointNumber;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "elevation")
    private Double elevation;

    @Column(name = "local_sequence" , nullable = false)
    private Integer localSequence;

    @Column(name="global_sequence" , nullable = false)
    private Integer globalSequence;

    @Column(name="location" , columnDefinition = "Geometry(Point, 4326)")
    private Point location;

    @Column(name = "recorded_At")
    private LocalDateTime recordedAt;

    @Column(name = "geom", columnDefinition = "geometry(Point, 4326)")
    private Point geom;

    @OneToMany(mappedBy = "wayPoint" ,cascade = CascadeType.ALL,orphanRemoval = true ,fetch = FetchType.LAZY)
    private List<POI> pois = new ArrayList<>();
}
