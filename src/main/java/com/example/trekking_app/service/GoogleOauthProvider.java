package com.example.trekking_app.service;

import com.example.trekking_app.dto.oauth.OauthUserInfo;
import com.example.trekking_app.exception.auth.LoginFailedException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
public class GoogleOauthProvider {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${app.oauth.google.use-playground:false}")
    private boolean isPlaygroundGenerated;

    @Value("${app.oauth.google.playground.client-id}")
    private String playgroundClientId;


    public OauthUserInfo verify(@NonNull String token) {
        try {
            log.info("Verifying id_token : "+token);
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(isPlaygroundGenerated ? playgroundClientId : clientId))
                    .build();


            GoogleIdToken googleIdToken = verifier.verify(token);

            if (googleIdToken == null) {
                throw new RuntimeException("Invalid Google token");
            }
            log.info("Google Id Token : "+ googleIdToken);

            GoogleIdToken.Payload payload = googleIdToken.getPayload();

            return OauthUserInfo.builder()
                    .email(payload.getEmail())
                    .name((String) payload.get("name"))
                    .provider("GOOGLE")
                    .providerId(payload.getSubject())
                    .build();

        } catch (Exception e) {
            log.error("App Google Oauth failed : {}",e.getLocalizedMessage());
            log.error("exception details :{}",e.getMessage());
            log.error("Error type : {}",e.getClass().getName());
            throw new LoginFailedException("Google token verification failed ");
        }
    }
}