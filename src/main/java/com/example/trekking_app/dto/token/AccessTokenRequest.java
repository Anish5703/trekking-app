package com.example.trekking_app.dto.token;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AccessTokenRequest {

    @NonNull
    @Size(min = 8,message = "refresh token is longer than 8 character")
    private String refreshToken;
}
