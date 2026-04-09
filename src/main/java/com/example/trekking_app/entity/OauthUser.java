package com.example.trekking_app.entity;

import jakarta.persistence.*;

@Entity
@Table(name="user_oauth")
public class OauthUser extends User{


    @Column(nullable = false)
    private String provider;

    public String getProvider(){return provider;}

    public void setProvider(String provider){ this.provider = provider;}
}
