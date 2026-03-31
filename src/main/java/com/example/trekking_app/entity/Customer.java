package com.example.trekking_app.entity;


import com.example.trekking_app.model.Role;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name="customer_tbl")
public class Customer extends User{

    public Customer() {
        super();
    }

    public Customer(String name, String email, String password, String contact, Role role )
    {
        super(name, email, password, contact, role);
    }
}
