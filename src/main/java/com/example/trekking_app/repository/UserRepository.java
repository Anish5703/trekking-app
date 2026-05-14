package com.example.trekking_app.repository;

import com.example.trekking_app.entity.User;
import com.example.trekking_app.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Integer> {

    Optional<User> findById(int id);
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = false ORDER BY u.timeStamp DESC")
    Page<User> findByRoleAndIsActiveFalseOrderByTimeStampDesc(@Param("role")Role role, Pageable pageable);
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true ORDER BY u.timeStamp DESC")
    Page<User> findByRoleAndIsActiveTrueOrderByTimeStampDesc(@Param("role") Role role, Pageable pageable);
    @Query("SELECT u FROM User u WHERE u.role = :role ORDER BY u.timeStamp DESC")
    Page<User> findByRoleOrderByTimeStampDesc(@Param("role") Role role , Pageable pageable);
    boolean existsByEmail(String email);
    long count();
}
