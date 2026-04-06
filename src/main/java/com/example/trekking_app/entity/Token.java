package com.example.trekking_app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "token_tbl")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = "user")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String tokenName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Token(String tokenName, User user) {
        this.tokenName = tokenName;
        this.user = user;
    }
}