package com.example.trekking_app.entity;


import com.example.trekking_app.model.OfflineRegionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "offline_regions")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OfflineRegion extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id" , nullable = false , unique = true)
    private Route route;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false , length = 20)
    private OfflineRegionStatus status;

    @Column(name = "max_latitude" , nullable = false)
    private Double maxLatitude;

    @Column(name = "min_latitude" , nullable = false)
    private Double minLatitude;

    @Column(name = "min_longitude" , nullable = false)
    private Double minLongitude;

    @Column(name = "max_longitude" , nullable = false)
    private Double maxLongitude;



    @Column(name = "bundle_url" , nullable = false)
    private String bundleUrl;



}
