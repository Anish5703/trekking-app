package com.example.trekking_app.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
@Table(name = "waypoints")
@Entity
public class WayPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="route_id")
    private Route route;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name="sequence_order" , nullable = false)
    private int sequenceOrder;

    @OneToMany(mappedBy = "waypoint" ,cascade = CascadeType.ALL,orphanRemoval = true)
    private List<POI> pois = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime timeStamp;
}
