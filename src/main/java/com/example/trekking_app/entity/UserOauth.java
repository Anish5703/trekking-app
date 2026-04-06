package com.example.trekking_app.entity;

import jakarta.persistence.*;

@Entity
@Table(name="user_oauth")
public class UserOauth extends User{


    @Column(nullable = false)
    private String provider;

    @Column(nullable = false,unique = true)
    private String token;
}
