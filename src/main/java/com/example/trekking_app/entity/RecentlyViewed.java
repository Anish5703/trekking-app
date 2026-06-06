package com.example.trekking_app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "recently_viewed" , indexes = {
        @Index(name = "idx_recently_viewed_user" , columnList = "user_id"),
                @Index(name = "idx_recently_viewed_route" , columnList = "route_id"),
        @Index(name = "idx_recently_viewed_user_route" , columnList = "user_id,route_id")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RecentlyViewed extends BaseEntity{

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id",nullable = false)
    private Route route;

    @Column(name = "counter")
    private Integer counter;
}
