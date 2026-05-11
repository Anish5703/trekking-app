package com.example.trekking_app.dto.oauth;

import com.example.trekking_app.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OauthLoginRequest {

    @NonNull
    @Size(min = 20 , message = "token is too short")
    private String token;
    @NotBlank
    @Size(min = 3 , max = 20 , message = "provider name must be between 3 - 20 characters")
    private String provider;
    @Builder.Default
    private Role role = Role.CUSTOMER;
}
