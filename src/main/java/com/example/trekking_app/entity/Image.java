package com.example.trekking_app.entity;

import com.example.trekking_app.model.EntityType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name="original_name")
    private String originalName;

    @Column(name = "public_id") // cloudinary public_id for deletion
    private String publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType; // ROUTE, DESTINATION, POI, USER

    @Column(name = "entity_id", nullable = false)
    private Integer entityId;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;


}
