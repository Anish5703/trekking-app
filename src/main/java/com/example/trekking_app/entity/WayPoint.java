package com.example.trekking_app.entity;


import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "way_points" , uniqueConstraints = {
        @UniqueConstraint(columnNames = {"route_id" , "sequence_order"})
})
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

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "elevation")
    private Double elevation;


    @Column(name="global_sequence" , nullable = false)
    private Integer globalSequence;

    @Column(name="location" , columnDefinition = "Geometry(Point, 4326)")
    private Point location;

    @Column(name = "recorded_At")
    private LocalDateTime recordedAt;

    @OneToMany(mappedBy = "way_point" ,cascade = CascadeType.ALL,orphanRemoval = true ,fetch = FetchType.LAZY)
    private List<POI> pois = new ArrayList<>();
}
