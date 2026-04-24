package com.example.trekking_app.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "way_points" , uniqueConstraints = {
        @UniqueConstraint(columnNames = {"route_id" , "sequence_order"})
})
@Entity
@Builder
public class WayPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="route_id")
    private Route route;

    @Column(nullable = false , length = 100)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name="sequence_order" , nullable = false)
    private Integer sequenceOrder;

    @OneToMany(mappedBy = "wayPoint" ,cascade = CascadeType.ALL,orphanRemoval = true ,fetch = FetchType.LAZY)
    private List<POI> pois = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime timeStamp;
}
