package com.example.trekking_app.repository;

import com.example.trekking_app.entity.User;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Integer> {

    Optional<User> findById(int id);
    Optional<User> findByEmail(String email);
    Optional<User> findByContact(String contact);
    boolean existsByEmail(String email);
    long count();
}
