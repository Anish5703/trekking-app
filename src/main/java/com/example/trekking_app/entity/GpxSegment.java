package com.example.trekking_app.entity;


import com.example.trekking_app.model.GpxSegmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.aspectj.bridge.IMessage;

import java.time.LocalDateTime;

@Entity
@Table(name="gpx_segments")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class GpxSegment {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @Column(nullable = false , name ="source_file_name")
    private String sourceFileName;

    @Column(nullable = false , name="source_file_hash")
    private String sourceFileHash;

    @Column(nullable = false,name = "order_index")
    private int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,name = "status")
    private GpxSegmentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id" , nullable = false)
    private User user;

    @Column(name="recorded_at")
    private LocalDateTime recordedAt;

    @Column(name="recorded_until")
    private LocalDateTime recordedUntil;






}
