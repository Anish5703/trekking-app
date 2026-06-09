package com.example.trekking_app.repository;

import com.example.trekking_app.dto.image.ImageResponse;
import com.example.trekking_app.entity.Image;
import com.example.trekking_app.model.EntityType;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface ImageRepository extends JpaRepository<Image,Integer> {

    Boolean existsByOriginalNameAndEntityTypeAndEntityId(String originalName, EntityType entityType,int entityId);

    List<Image> findByEntityTypeAndEntityId(EntityType entityType, @NonNull Integer entityId);

    Set<String> findOriginalNamesByEntityTypeAndEntityId(@NonNull EntityType entityType, @NonNull Integer entityId);
}
