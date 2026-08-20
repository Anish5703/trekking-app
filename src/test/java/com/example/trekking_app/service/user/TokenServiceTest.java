package com.example.trekking_app.service.user;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.token.AccessTokenResponse;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.service.auth.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @InjectMocks
    private TokenService tokenService;

    @Mock
    private JwtService jwtService;

    @Mock
    private MyUserDetailsService userDetailsService;

    @Mock
    private UserDetails userDetails;

    @Test
    void generateJwtTokenSuccessfully() {
        String refreshToken = "validRefreshToken";
        String email = "test@email.com";
        String accessToken = "newAccessToken";

        when(jwtService.extractEmail(refreshToken)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.validateToken(refreshToken, userDetails)).thenReturn(true);
        when(jwtService.generateAccessToken(email)).thenReturn(accessToken);

        ApiResponse<AccessTokenResponse> response = tokenService.generateJwtToken(refreshToken);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals(accessToken, response.getData().getAccessToken());

        verify(jwtService).extractEmail(refreshToken);
        verify(userDetailsService).loadUserByUsername(email);
        verify(jwtService).validateToken(refreshToken, userDetails);
        verify(jwtService).generateAccessToken(email);
    }

    @Test
    void generateJwtTokenThrowsExceptionWhenEmailIsEmpty() {
        String refreshToken = "invalidToken";

        when(jwtService.extractEmail(refreshToken)).thenReturn("");

        assertThrows(JwtException.class, () -> tokenService.generateJwtToken(refreshToken));
        verify(jwtService).extractEmail(refreshToken);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void generateJwtTokenThrowsExceptionWhenTokenIsInvalid() {
        String refreshToken = "expiredToken";
        String email = "test@email.com";

        when(jwtService.extractEmail(refreshToken)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtService.validateToken(refreshToken, userDetails)).thenReturn(false);

        JwtException exception = assertThrows(
                JwtException.class,
                () -> tokenService.generateJwtToken(refreshToken)
        );

        assertEquals(
                "refresh token is either invalid or expired",
                exception.getMessage()
        );

        verify(jwtService).validateToken(refreshToken, userDetails);
        verify(jwtService, never()).generateAccessToken(anyString());
    }

    @Test
    void generateRefreshTokenSuccessfully() {
        User user = new User();
        user.setEmail("test@email.com");

        String refreshToken = "generatedRefreshToken";

        when(jwtService.generateRefreshToken(user.getEmail()))
                .thenReturn(refreshToken);

        String result = tokenService.generateRefreshToken(user);

        assertEquals(refreshToken, result);

        verify(jwtService).generateRefreshToken(user.getEmail());
    }

    @Test
    void generateRefreshTokenThrowsExceptionWhenEmailIsNull() {
        User user = new User();
        user.setEmail(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tokenService.generateRefreshToken(user)
        );

        assertEquals("email field empty", exception.getMessage());

        verify(jwtService, never()).generateRefreshToken(anyString());
    }
}