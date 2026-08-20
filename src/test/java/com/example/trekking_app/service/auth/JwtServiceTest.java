package com.example.trekking_app.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private  static final String SECRET = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXphYmNkZWZnaGlqa2xtbm9wcQ==";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
    }

    @Test
    void shouldGenerateAccessToken() {
        String token = jwtService.generateAccessToken("test@example.com");

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldGenerateRefreshToken() {
        String token = jwtService.generateRefreshToken("test@example.com");

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldExtractEmailFromToken() {
        String email = "test@example.com";

        String token = jwtService.generateAccessToken(email);

        String extractedEmail = jwtService.extractEmail(token);

        assertEquals(email, extractedEmail);
    }

    @Test
    void shouldValidateTokenForCorrectUser() {
        String email = "test@example.com";

        String token = jwtService.generateAccessToken(email);

        UserDetails userDetails = User.withUsername(email)
                .password("password")
                .authorities("USER")
                .build();

        assertTrue(jwtService.validateToken(token, userDetails));
    }

    @Test
    void shouldNotValidateTokenForDifferentUser() {
        String token = jwtService.generateAccessToken("test@example.com");

        UserDetails userDetails = User.withUsername("other@example.com")
                .password("password")
                .authorities("USER")
                .build();

        assertFalse(jwtService.validateToken(token, userDetails));
    }
}