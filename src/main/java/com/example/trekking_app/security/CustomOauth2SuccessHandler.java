package com.example.trekking_app.security;

import com.example.trekking_app.dto.oauth.OauthSignupRequest;
import com.example.trekking_app.exception.auth.LoginFailedException;
import com.example.trekking_app.repository.UserRepository;
import com.example.trekking_app.service.JwtService;
import com.example.trekking_app.service.OauthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomOauth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OauthService oauthService;
    private final UserRepository userRepo;
    private final JwtService jwtService;
    private final String frontendUrl;


    public CustomOauth2SuccessHandler(OauthService oauthService,UserRepository userRepo,
                                        JwtService jwtService)
    {
        this.oauthService =  oauthService;
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.frontendUrl = "/api/oauth/login";
    }


    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        if(oauth2User == null)
        {
            throw new LoginFailedException("Failed to authenticate user");
        }
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String provider = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();

        String jwtToken = jwtService.generateToken(email);
        //Check if email is already registered
        if(userRepo.existsByEmail(email))
          response = oauthService.setJwtCookieAndHeader(request,response,jwtToken);

        else
        {
            OauthSignupRequest signupRequest = new OauthSignupRequest(email,provider,name);
            oauthService.signupUser(signupRequest);
            response = oauthService.setJwtCookieAndHeader(request,response,jwtToken);
        }
        response.sendRedirect(frontendUrl);
    }
}
