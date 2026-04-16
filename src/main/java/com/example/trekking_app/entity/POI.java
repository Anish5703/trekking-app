package com.example.trekking_app.entity;

import com.example.trekking_app.model.POIType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="pois" , indexes = {
        @Index( name = "idx_poi_route" , columnList = "route_id"),
        @Index(name = "idx_poi_type" , columnList = "route_id , type" ),
        @Index(name = "idx_poi_lat_lon" , columnList = "latitude , longitude")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class POI extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "waypoint_id")
    private WayPoint wayPoint;

    @Column(nullable = false,length = 100)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double elevation;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private POIType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "poi" , cascade = CascadeType.ALL , orphanRemoval = true,fetch = FetchType.LAZY)
    private List<Image> images = new ArrayList<>();





}
