package com.example.trekking_app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "track_points" , indexes = {
        @Index(name  = "idx_track_points_route_seq" , columnList = "route_id , sequence_order"),
        @Index(name = "idx_track_points_geom" , columnList = "geom")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackPoint extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="route_id")
    private Route route;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column
    private Double elevation;

    @Column(name = "sequence_order",nullable = false)
    private Integer sequenceOrder;

    @Column(name = "geom" , columnDefinition = "geometry(Point, 4326)")
    private Point geom;

    @Column(name = "time_stamp")
    private LocalDateTime timeStamp;
}
