package com.example.trekking_app.service.auth;

import com.example.trekking_app.dto.oauth.OauthUserInfo;
import com.example.trekking_app.exception.auth.LoginFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacebookOauthProviderTest {

    private FacebookOauthProvider facebookOauthProvider;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setup() {
        facebookOauthProvider = new FacebookOauthProvider();

        ReflectionTestUtils.setField(facebookOauthProvider, "appId", "test-app-id");
        ReflectionTestUtils.setField(facebookOauthProvider, "appSecret", "test-secret");
        ReflectionTestUtils.setField(facebookOauthProvider, "restTemplate", restTemplate);
    }

    @Test
    void shouldVerifySuccessfully() {

        Map<String, Object> debugData = Map.of(
                "is_valid", true,
                "app_id", "test-app-id"
        );

        Map<String, Object> debugResponse = Map.of(
                "data", debugData
        ); //verifyToken() get response

        Map<String, Object> userResponse = Map.of("id", "123", "name", "test", "email", "test@email.com");

        when(restTemplate.getForObject(contains("debug_token"), eq(Map.class))).thenReturn(debugResponse);

        when(restTemplate.getForObject(contains("/me"),eq(Map.class))).thenReturn(userResponse);

        OauthUserInfo result = facebookOauthProvider.verify("token");

        assertEquals("123", result.getProviderId());
        assertEquals("test", result.getName());
        assertEquals("test@email.com", result.getEmail());
        assertEquals("FACEBOOK", result.getProvider());
    }

    //login failed exception
    @Test
    void shouldThrowWhenTokenIsInvalid() {

        Map<String, Object> debugData = Map.of(
                "is_valid", false,
                "app_id", "test-app-id"
        );

        Map<String, Object> debugResponse = Map.of(
                "data", debugData
        );

        when(restTemplate.getForObject(contains("debug_token"), eq(Map.class)))
                .thenReturn(debugResponse);

        assertThrows(LoginFailedException.class, () -> facebookOauthProvider.verify("token"));
    }

    //login failed exception
    @Test
    void shouldThrowWhenTokenBelongsToDifferentApp() {

        Map<String, Object> debugData = Map.of(
                "is_valid", true,
                "app_id", "other-app-id"
        );

        Map<String, Object> debugResponse = Map.of(
                "data", debugData
        );

        when(restTemplate.getForObject(contains("debug_token"), eq(Map.class)))
                .thenReturn(debugResponse);

        assertThrows(LoginFailedException.class, () -> facebookOauthProvider.verify("token"));
    }

    //login failed exception
    @Test
    void shouldThrowWhenUserInfoIsNull() {

        Map<String, Object> debugData = Map.of(
                "is_valid", true,
                "app_id", "test-app-id"
        );

        Map<String, Object> debugResponse = Map.of(
                "data", debugData
        );

        when(restTemplate.getForObject(contains("debug_token"), eq(Map.class)))
                .thenReturn(debugResponse);

        when(restTemplate.getForObject(contains("/me"), eq(Map.class)))
                .thenReturn(null);

        assertThrows(LoginFailedException.class, () -> facebookOauthProvider.verify("token"));
    }

    // login failed exception

    @Test
    void shouldThrowWhenFacebookApiThrowsException() {

        when(restTemplate.getForObject(contains("debug_token"), eq(Map.class)))
                .thenThrow(new RuntimeException("Facebook error"));

        assertThrows(LoginFailedException.class, () -> facebookOauthProvider.verify("token"));
    }
}