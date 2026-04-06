package com.example.trekking_app.repository;

import com.example.trekking_app.entity.OauthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OauthUserRepository extends JpaRepository<OauthUser,Integer> {

    Optional<OauthUser> findById(int id);
    Optional<OauthUser> findByEmail(String email);
}
