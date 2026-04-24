package com.example.trekking_app.entity;


import com.example.trekking_app.model.TrailType;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.LineString;

@Entity
@Table(name = "trail_segments" , indexes = {
        @Index(name = "idx_trail_seg_route" , columnList = "route_id"),
        @Index(name = "idx_trail_seg_type" , columnList = "route_id , type"),
        @Index(name = "idx_trail_seg_geom" , columnList = "segment_geometry")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrailSegment extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false , length = 30)
    private TrailType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_waypoint_id" , nullable = false)
    private WayPoint startWaypoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_waypoint_id" , nullable = false)
    private WayPoint endWaypoint;

    @Column(length = 10)
    private String color;

    @Column(name = "segment_group" , length = 100)
    private String segmentGroup;

    @Column(name = "priority_order")
    @Builder.Default
    private Integer priorityOrder = 0;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = true;

    @Column(name = "estimated_time_minutes")
    private Integer estimatedTimeMinutes;

    /* distance in km */
    @Column(name = "distance")
    private Double distance;

    @Column(name = "segment_geometry" , columnDefinition = "geometry(LineString, 4326)")
    private LineString segmentGeometry;
}
