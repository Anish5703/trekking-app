package com.example.trekking_app.dto.oauth;

import com.example.trekking_app.model.Role;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;
import lombok.NoArgsConstructor;

@Hidden
@NoArgsConstructor
@Data
public class OauthSignupResponse {

    private int id;
    private String name;
    private String email;
    private String provider;
    private Role role;
}
