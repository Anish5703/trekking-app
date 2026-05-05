package com.example.trekking_app.entity;

import com.example.trekking_app.model.TrackPointStatus;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "track_points" , indexes = {
        @Index(name  = "idx_track_points_route_seq" , columnList = "route_id , global_sequence"),
        @Index(name = "idx_track_points_route_deleted_seq",
                columnList = "route_id, is_deleted, global_sequence"),
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="gpx_segment_id")
    private GpxSegment gpxSegment;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column
    private Double elevation;

    @Column(name = "local_sequence",nullable = false)
    private Integer localSequence;

    @Column(name = "global_sequence")
    @Builder.Default
    private Integer globalSequence = 0;


    @Column(name = "geom" , columnDefinition = "geometry(Point, 4326)")
    private Point geom;

    @Enumerated(EnumType.STRING)
    @Column(name ="status")
    @Builder.Default
    private TrackPointStatus status = TrackPointStatus.ACTIVE;

    @Column(name="recorded_at" )
    private LocalDateTime recordedAt;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;


}
