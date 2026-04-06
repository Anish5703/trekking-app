package com.example.trekking_app.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class LoginRequest {

    @NotBlank(message = "email cannot be empty")
    private String email;
    @NotBlank(message="password cannot be empty")
    private String password;
}
