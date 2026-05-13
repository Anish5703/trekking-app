package com.example.trekking_app.dto.auth;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ForgotPasswordResetRequest {

    @NotBlank
    @Min(value = 8 , message="token is usually at least 8 character")
    private String token;
    @NotBlank(message = "new password required")
    @Min(value = 5 ,message = "password must be at least 5 character")
    private String newPassword;

}
