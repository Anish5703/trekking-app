package com.example.trekking_app.dto.oauth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OauthSignupRequest {

    String email;
    String provider;
    String name;

}
