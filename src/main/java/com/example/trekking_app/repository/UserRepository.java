package com.example.trekking_app.repository;

import com.example.trekking_app.entity.User;
import com.example.trekking_app.model.Role;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Integer> {

    Optional<User> findById(int id);
    Optional<User> findByEmail(String email);
    Optional<User> findByContact(String contact);
    Page<User> findByRoleAndIsActiveFalse(Role role, Pageable pageable);
    Page<User> findByRoleAndIsActiveTrue(Role role,Pageable pageable);
    boolean existsByEmail(String email);
    long count();
}
