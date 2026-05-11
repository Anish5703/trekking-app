package com.example.trekking_app.repository;

import com.example.trekking_app.entity.OauthUser;
import com.example.trekking_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface OauthUserRepository extends JpaRepository<OauthUser,Integer> {

    Optional<OauthUser> findById(int id);
    Optional<OauthUser> findByEmail(String email);
    boolean existsByEmail(String email);
    
    boolean existsByProviderAndProviderId(String provider, String providerId);

    Optional<OauthUser> findByProviderAndProviderId(String provider, String providerId);
}
