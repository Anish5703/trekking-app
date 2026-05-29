package com.example.trekking_app.entity;


import com.example.trekking_app.model.TrailType;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.LineString;

@Entity
@Table(name = "trail_segments" , indexes = {
        @Index(name = "idx_trail_seg_route" , columnList = "route_id"),
        @Index(name = "idx_trail_seg_type" , columnList = "route_id , type"),
        @Index(name = "idx_trail_seg_path" , columnList = "path")
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

    @Column
    private String name;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gpx_segment_id")
    private GpxSegment gpxSegment;


    @Column(name = "estimated_time_minutes")
    private Integer estimatedTimeMinutes;

    /* distance in meter */
    @Column(name = "distance")
    private Double distanceInMeter;

    @Column(name = "path" , columnDefinition = "geometry(LineString, 4326)")
    private LineString path;
}
