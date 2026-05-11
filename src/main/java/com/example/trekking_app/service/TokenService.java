package com.example.trekking_app.service;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.token.AccessTokenResponse;
import com.example.trekking_app.entity.User;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JwtService jwtService;
    private final MyUserDetailsService userDetailsService;

    public ApiResponse<AccessTokenResponse> generateJwtToken(@NonNull String refreshToken)
    {

        String email = jwtService.extractEmail(refreshToken);
        if(email.isEmpty()) throw new JwtException("failed to fetch email from refresh token");

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if(!jwtService.validateToken(refreshToken,userDetails))
            throw new JwtException("refresh token is either invalid or expired");

        String jwt = jwtService.generateAccessToken(email);
        AccessTokenResponse accessTokenResponse = AccessTokenResponse.builder().accessToken(jwt).build();
        return new ApiResponse<>(accessTokenResponse,"new jwt token generated ! expires after 30 minutes",200);

    }

    public String generateRefreshToken(@NonNull User user)
    {

        if(user.getEmail()==null)
            throw new IllegalArgumentException("email field empty");
        return jwtService.generateRefreshToken(user.getEmail());
    }
}
