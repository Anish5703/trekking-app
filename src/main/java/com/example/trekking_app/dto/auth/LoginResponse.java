package com.example.trekking_app.dto.auth;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class LoginResponse {

    private int userId;
    private String name;
    private String email;
    private String jwtToken;

}
