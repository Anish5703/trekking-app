package com.example.trekking_app.security;

import com.example.trekking_app.exception.auth.LoginFailedException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOauth2FailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException,LoginFailedException {

        /*
        // JSON response
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"OAuth login failed CustomOauth2FailureHandler: " + exception.getMessage() + "\"}");
        */
        throw new LoginFailedException("Oauth login failed : access denied");
    }
    }
