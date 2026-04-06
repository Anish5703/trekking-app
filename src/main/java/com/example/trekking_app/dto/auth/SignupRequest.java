package com.example.trekking_app.dto.auth;

import com.example.trekking_app.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class SignupRequest {

    @NotBlank(message="name field is required")
    private String name;

    @Email(message="email field is required")
    private String email;

    @NotBlank(message ="password field is required")
    @Size(min = 5 ,message = "password must be at least 5 characters")
    private String password;

    @NotBlank(message="contact field is required")
    @Size(min=10,message="contact number must be at least 10 character")
    private String contact;
    @NotNull(message="role is required")
    private Role role;
}
