package com.example.trekking_app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Column(nullable = false,updatable = false)
    @CreationTimestamp
    private LocalDateTime timeStamp;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
