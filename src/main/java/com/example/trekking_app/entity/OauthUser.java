package com.example.trekking_app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;


@Entity
@Table(name="oauth_users")
public class OauthUser extends User{


    @Column(nullable = false)
    private String provider;

    @Column(unique = true)
    private String providerId;

    public String getProvider(){return provider;}

    public void setProvider(String provider){ this.provider = provider;}

    public String getProviderId(){return providerId;}

    public void setProviderId(String providerId){this.providerId = providerId;}
}
