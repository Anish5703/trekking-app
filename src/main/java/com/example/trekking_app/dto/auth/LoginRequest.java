package com.example.trekking_app.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class LoginRequest {

    @NotBlank(message = "email cannot be empty")
    @Email
    private String email;
    @NotBlank(message="password cannot be empty")
    @Size(min=5,message = "password is at least 5 character")
    private String password;
}
