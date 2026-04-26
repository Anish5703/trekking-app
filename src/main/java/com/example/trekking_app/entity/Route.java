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
})
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

    @Column(name = "route_geometry",columnDefinition = "geometry(LineString, 4326)")
    private LineString routeGeometry;

    @Column(name="max_elevation")
    private Double maxElevation;

    @Column(name = "min_elevation")
    private Double minElevation;

    @Column(name="estimated_days")
    private Integer estimatedDays;

    // distance in km
    @Column
    private double distanceInKm;

    @Column(name="difficulty_level",length=20)
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel;


    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL , orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    private List<WayPoint> waypoints = new ArrayList<>();

    @OneToMany(mappedBy = "route",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrackPoint> trackPoints = new ArrayList<>();

    @OneToMany(mappedBy = "route" ,cascade = CascadeType.ALL , orphanRemoval = true)
    private List<POI> pois = new ArrayList<>();

    @OneToMany(mappedBy = "route" ,cascade = CascadeType.ALL , orphanRemoval = true)
    private List<DangerZone> dangerZones = new ArrayList<>();

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL , orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

   @OneToOne(mappedBy = "route" , cascade = CascadeType.ALL , orphanRemoval = true )
   private List<OfflineRegion> offlineRegions = new ArrayList<>();





}
