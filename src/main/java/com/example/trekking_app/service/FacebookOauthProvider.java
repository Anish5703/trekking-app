package com.example.trekking_app.service;

import com.example.trekking_app.dto.oauth.OauthUserInfo;
import com.example.trekking_app.exception.auth.LoginFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class FacebookOauthProvider {

    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String appId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-secret}")
    private String appSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public OauthUserInfo verify(String token) {
        try {
            // 1. Verify token is valid and belongs to your app
            verifyToken(token);

            // 2. Fetch user info from Facebook Graph API
            String userInfoUrl = "https://graph.facebook.com/me"
                    + "?fields=id,name,email,picture"
                    + "&access_token=" + token;

            Map<String, Object> userInfo = restTemplate.getForObject(userInfoUrl, Map.class);

            if (userInfo == null)
                throw new RuntimeException("Failed to fetch user info from Facebook");

            // extract picture url
            String pictureUrl = extractPictureUrl(userInfo);

            return OauthUserInfo.builder()
                    .email((String) userInfo.get("email"))
                    .name((String) userInfo.get("name"))
                    .provider("FACEBOOK")
                    .providerId((String) userInfo.get("id"))
                    .build();

        } catch (Exception e) {
            log.error("App Facebook Oauth failed : {}", e.getLocalizedMessage());
            throw new LoginFailedException("Facebook token verification failed");
        }
    }

    // Verify token with Facebook debug_token endpoint
    private void verifyToken(String token) {
        String appToken = appId + "|" + appSecret;
        String inspectUrl = "https://graph.facebook.com/debug_token"
                + "?input_token=" + token
                + "&access_token=" + appToken;

        Map<String, Object> response = restTemplate.getForObject(inspectUrl, Map.class);

        if (response == null)
            throw new RuntimeException("Failed to verify Facebook token");

        Map<String, Object> data = (Map<String, Object>) response.get("data");

        if (data == null)
            throw new RuntimeException("Invalid Facebook token response");

        boolean isValid = (boolean) data.get("is_valid");
        if (!isValid)
            throw new RuntimeException("Facebook token is invalid");

        // check token belongs to your app
        String tokenAppId = (String) data.get("app_id");
        if (!tokenAppId.equals(appId))
            throw new RuntimeException("Facebook token does not belong to this app");
    }

    // Extract picture URL from nested response
    private String extractPictureUrl(Map<String, Object> userInfo) {
        try {
            Map<String, Object> picture = (Map<String, Object>) userInfo.get("picture");
            Map<String, Object> pictureData = (Map<String, Object>) picture.get("data");
            return (String) pictureData.get("url");
        } catch (Exception e) {
            return null;
        }
    }
}