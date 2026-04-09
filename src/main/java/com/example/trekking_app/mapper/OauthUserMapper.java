package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.oauth.OauthSignupRequest;
import com.example.trekking_app.dto.oauth.OauthLoginResponse;
import com.example.trekking_app.entity.OauthUser;
import com.example.trekking_app.model.Role;
import org.springframework.security.crypto.keygen.KeyGenerators;

public class OauthUserMapper {

    public OauthSignupRequest toOauthSignupRequest(OauthUser oauthUser)
    {
         OauthSignupRequest signupRequest = new OauthSignupRequest();
         signupRequest.setName(oauthUser.getName());
         signupRequest.setEmail(oauthUser.getEmail());
         signupRequest.setProvider(oauthUser.getProvider());
         return signupRequest;
    }
    public OauthUser toOauthUser(OauthSignupRequest oauthSignupRequest)
    {
        OauthUser oauthUser = new OauthUser();
        oauthUser.setName(oauthSignupRequest.getName());
        oauthUser.setEmail(oauthSignupRequest.getEmail());
        oauthUser.setRole(Role.CUSTOMER);
        oauthUser.setContact(null);
        oauthUser.setProvider(oauthSignupRequest.getProvider());
        oauthUser.setPassword(KeyGenerators.string().generateKey());
        oauthUser.setEmailVerified(true);
        return oauthUser;
    }

    public OauthLoginResponse toOauthUserDetails(OauthUser oauthUser)
    {
        OauthLoginResponse oauthLoginResponse = new OauthLoginResponse();
        oauthLoginResponse.setId(oauthUser.getId());
        oauthLoginResponse.setName(oauthUser.getName());
        oauthLoginResponse.setEmail(oauthUser.getEmail());
        oauthLoginResponse.setRole(oauthUser.getRole());
        oauthLoginResponse.setProvider(oauthUser.getProvider());
        return oauthLoginResponse;
    }
}
