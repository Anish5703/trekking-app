package com.example.trekking_app.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PasswordResetRequest {

    @NotBlank(message = "old password required")
    @Size(min= 5 , message = "password is at least 5 character")
    private String oldPassword;
    @NotBlank(message = "new password required")
    @Size(min=5 , message = "password must be at least 5 character")
    private String newPassword;
}
