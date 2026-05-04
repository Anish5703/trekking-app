package com.example.trekking_app.entity;

import com.example.trekking_app.model.DifficultyLevel;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.LineString;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="routes",
indexes = {
        @Index(name="idx_routes_name",columnList = "name"),
        @Index(name="idx_routes_user" , columnList = "user_id")
},
        uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name","destination_id"})
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Route extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false,length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id" , nullable = false)
    private Destination destination;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "path",columnDefinition = "geometry(LineString, 4326)")
    private LineString path;

    @Column(name="max_elevation")
    private Double maxElevation;

    @Column(name = "min_elevation")
    private Double minElevation;

    @Column(name="estimated_days")
    private Integer estimatedDays;

    @Column
    private double distanceInKm;

    @Column(name="difficulty_level",length=20)
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel;


    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL , orphanRemoval = true , fetch = FetchType.LAZY)
    @OrderBy("global_sequence ASC")
    private List<TrackPoint> trackPoints = new ArrayList<>();

    @OneToMany(mappedBy = "route",cascade = CascadeType.ALL, orphanRemoval = true , fetch = FetchType.LAZY)
    @OrderBy("order_index ASC")
    private List<GpxSegment> gpxSegments = new ArrayList<>();

    @OneToMany(mappedBy = "route" ,cascade = CascadeType.ALL , orphanRemoval = true , fetch = FetchType.LAZY)
    private List<POI> pois = new ArrayList<>();

    @OneToMany(mappedBy = "route" ,cascade = CascadeType.ALL , orphanRemoval = true , fetch = FetchType.LAZY)
    private List<DangerZone> dangerZones = new ArrayList<>();

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL , orphanRemoval = true , fetch = FetchType.LAZY)
    private List<Image> images = new ArrayList<>();

   @OneToOne(mappedBy = "route" , cascade = CascadeType.ALL , orphanRemoval = true )
   private List<OfflineRegion> offlineRegions = new ArrayList<>();





}
