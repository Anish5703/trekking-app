package com.example.trekking_app.dto.oauth;

import com.example.trekking_app.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OauthUserInfo {

    private String email;
    private String name ;
    private String provider;
    private String providerId;
    private Role role;
}
