package com.example.trekking_app.dto.oauth;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class OauthSignupResponse {

    private int id;
    private String name;
    private String email;
    private String provider;
}
