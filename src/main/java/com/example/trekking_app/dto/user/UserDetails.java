package com.example.trekking_app.dto.user;

import com.example.trekking_app.model.Role;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class UserDetails {
    private int id;
    private String name;
    private String email;
    private String contact;
    private Role role;
}
