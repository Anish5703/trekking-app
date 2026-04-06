package com.example.trekking_app.dto.auth;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SignupResponse {
    private int id;
    private String name;
    private String email;
    private String contact;
    private Role role;

}
