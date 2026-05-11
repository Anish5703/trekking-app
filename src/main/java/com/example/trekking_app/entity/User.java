package com.example.trekking_app.entity;

import com.example.trekking_app.exception.auth.EmptySignupFieldException;
import com.example.trekking_app.model.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Table(name="users")
@Inheritance(strategy = InheritanceType.JOINED)
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false)
    private String name;
    @Email
    @Column(unique = true,nullable = false)
    private String email;
    private String password;
    private String contact;
    @Enumerated(EnumType.STRING)
    private Role role;
    private boolean emailVerified = false;
    @CreationTimestamp
    private LocalDateTime timeStamp;


    public User(String name, String email, String password, String contact, Role role)
    {
        if(name==null || email == null)
            throw new EmptySignupFieldException("User fields cannot be empty");
        this.name= name;
        this.email = email;
        this.password = password;
        this.contact = contact;
        this.role = role;


    }
}
