package com.example.trekking_app.repository;

import com.example.trekking_app.dto.image.ImageResponse;
import com.example.trekking_app.entity.Image;
import com.example.trekking_app.model.EntityType;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ImageRepository extends JpaRepository<Image,Integer> {

    Boolean existsByOriginalNameAndEntityTypeAndEntityId(String originalName, EntityType entityType,int entityId);

    List<Image> findByEntityTypeAndEntityId(EntityType entityType, @NonNull Integer entityId);

    @Query("SELECT i.originalName FROM Image i WHERE i.entityType = :entityType AND i.entityId = :entityId")
    Set<String> findOriginalNamesByEntityTypeAndEntityId(@Param("entityType") EntityType entityType,
                                                         @Param("entityId") Integer entityId);

    boolean existsByEntityTypeAndEntityId(EntityType entityType, Integer id);

    void deleteByEntityTypeAndEntityId(EntityType entityType, Integer entityId);
}
