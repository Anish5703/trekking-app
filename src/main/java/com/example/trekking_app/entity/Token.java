package com.example.trekking_app.entity;

import com.google.api.client.util.DateTime;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tokens")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = "user")
@Builder
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String tokenName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expiry_at")
    private LocalDateTime expiryAt;


    public Token(String tokenName, User user) {
        this.tokenName = tokenName;
        this.user = user;
    }
    public boolean isExpired()
    {
        if(expiryAt==null) return true;
        return LocalDateTime.now().isAfter(expiryAt);
    }
}