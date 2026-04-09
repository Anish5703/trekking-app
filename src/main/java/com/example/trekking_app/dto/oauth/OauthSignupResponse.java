package com.example.trekking_app.dto.oauth;

import com.example.trekking_app.model.Role;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class OauthSignupResponse {

    private int id;
    private String name;
    private String email;
    private String provider;
    private Role role;
}
