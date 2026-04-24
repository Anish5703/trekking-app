package com.example.trekking_app.entity;

import jakarta.persistence.*;
import lombok.*;

@Table(name ="destination",
uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name","district","region"})
})
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false  , length = 100)
    private String name ;

    @Column(nullable = false , length = 100)
    private String district;

    @Column(nullable = false , length = 100)
    private String region;
}
