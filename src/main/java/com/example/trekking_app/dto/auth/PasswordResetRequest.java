package com.example.trekking_app.dto.auth;

import jakarta.validation.constraints.NotBlank;
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
    private String oldPassword;
    @NotBlank(message = "new password required")
    private String newPassword;
}
