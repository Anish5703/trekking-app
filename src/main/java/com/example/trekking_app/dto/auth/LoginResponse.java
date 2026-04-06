package com.example.trekking_app.dto.auth;

import com.example.trekking_app.model.Role;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class LoginResponse {

    private int id;
    private String name;
    private String email;
    private String contact;
    private Role role;
    private String jwtToken;

}
