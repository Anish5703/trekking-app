package com.example.trekking_app.dto.oauth;

import com.example.trekking_app.dto.auth.LoginResponse;
import com.example.trekking_app.model.Role;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OauthLoginResponse extends LoginResponse {

    private String provider;


}
