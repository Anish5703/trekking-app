package com.example.trekking_app.entity;

import com.example.trekking_app.model.Role;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name="admin_tbl")
public class Admin extends User{

    public Admin(){}

    public Admin(String name, String email, String password, String contact, Role role )
    {
        super(name,email,password,contact,role);
    }
}
