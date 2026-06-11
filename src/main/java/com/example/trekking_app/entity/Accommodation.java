package com.example.trekking_app.entity;

import com.example.trekking_app.model.ElectricitySource;
import com.example.trekking_app.model.POIType;
import com.example.trekking_app.model.WaterSource;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "accommodations",indexes = {
        @Index( name = "idx_accommodation_route" , columnList = "route_id"),
        @Index(name = "idx_accommodation_type" , columnList = "route_id , type" ),
        @Index(name = "idx_accommodation_lat_lon" , columnList = "latitude , longitude")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Accommodation{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "way_point_id")
    private WayPoint wayPoint;

    @Column(nullable = false,length = 100)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double elevation;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private POIType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String address;

    @Column(name = "contact_number" , length = 50)
    private String contactNumber;

    @Column(name = "total_rooms")
    private Integer totalRooms;

    @Column(name = "price_nepali")
    private Double priceNepali;

    @Column(name = "price_foreigner")
    private Double priceForeigner;

    @Enumerated(EnumType.STRING)
    @Column(name = "electricity_source" , length = 50)
    private ElectricitySource electricitySource;

    @Enumerated(EnumType.STRING)
    @Column(name = "water_source" , length = 50)
    private WaterSource waterSource;

    @Column(name = "has_first_aid")
    private Boolean hasFirstAid;



}
