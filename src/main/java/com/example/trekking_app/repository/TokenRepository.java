package com.example.trekking_app.repository;

import com.example.trekking_app.entity.Token;
import com.example.trekking_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token,Integer> {
    Optional<Token> findByTokenName(String tokenName);
    Optional<Token> findByUser(User user);
    Optional<Token> findByUserName(String userName);
    void deleteByUserEmail(String userEmail);
}
