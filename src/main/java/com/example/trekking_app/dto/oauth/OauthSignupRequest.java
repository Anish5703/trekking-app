package com.example.trekking_app.dto.oauth;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Hidden
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OauthSignupRequest {

    String email;
    String provider;
    String name;

}
