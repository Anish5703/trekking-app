package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.oauth.OauthSignupRequest;
import com.example.trekking_app.dto.oauth.OauthLoginResponse;
import com.example.trekking_app.dto.oauth.OauthUserInfo;
import com.example.trekking_app.entity.OauthUser;
import com.example.trekking_app.entity.User;
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

    public OauthUser toOauthUser(OauthUserInfo userInfo)
    {
        OauthUser oauthUser = new OauthUser();
        oauthUser.setEmail(userInfo.getEmail());
        oauthUser.setName(userInfo.getName());
        oauthUser.setContact(null);
        oauthUser.setRole(userInfo.getRole());
        oauthUser.setPassword(KeyGenerators.string().generateKey());
        oauthUser.setProvider(userInfo.getProvider());
        oauthUser.setProviderId(userInfo.getProviderId());
        return oauthUser;
    }

    public OauthUser toOauthUser(User user , OauthUserInfo userInfo)
    {
        OauthUser oauthUser = new OauthUser();
        // Copy existing user fields
        oauthUser.setName(user.getName());
        oauthUser.setEmail(user.getEmail());
        String password = user.getPassword().isEmpty() ? KeyGenerators.string().generateKey() : user.getPassword();
        oauthUser.setPassword(password);
        oauthUser.setContact(user.getContact());
        oauthUser.setRole(user.getRole());
        oauthUser.setEmailVerified(true);

        // Set OAuth fields
        oauthUser.setProvider(userInfo.getProvider());
        oauthUser.setProviderId(userInfo.getProviderId());
        return oauthUser;
    }

    public OauthLoginResponse toOauthLoginResponse(OauthUser oauthUser)
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
