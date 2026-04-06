package com.example.trekking_app.entity;

import jakarta.persistence.*;

@Entity
@Table(name="user_oauth")
public class UserOauth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,unique = true)
    private User user;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false,unique = true)
    private String token;
}
