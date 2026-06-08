package com.example.trekking_app.repository;

import com.example.trekking_app.entity.Image;
import com.example.trekking_app.model.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image,Integer> {

    Boolean existsByOriginalNameAndEntityTypeAndEntityId(String originalName, EntityType entityType,int entityId);
}
