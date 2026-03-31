package com.example.trekking_app.repository;

import com.example.trekking_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Integer> {

    Optional<User> findById(int id);
    Optional<User> findByEmail(String email);
    Optional<User> findByContact(String contact);

    boolean existsByEmail(String email);
    long count();
}
