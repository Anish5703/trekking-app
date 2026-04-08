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
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class CustomOauth2SuccessHandler implements AuthenticationSuccessHandler {

    private final OauthService oauthService;
    private final UserRepository userRepo;
    private final JwtService jwtService;
    private final String redirectUrl;
    private final HandlerExceptionResolver handlerExceptionResolver;


    public CustomOauth2SuccessHandler(OauthService oauthService,UserRepository userRepo,
                                        JwtService jwtService,HandlerExceptionResolver handlerExceptionResolver)
    {
        this.oauthService =  oauthService;
        this.userRepo = userRepo;
        this.jwtService = jwtService;
        this.redirectUrl = "/api/oauth/login";
        this.handlerExceptionResolver = handlerExceptionResolver;
    }


    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        try {
            OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            if (oauth2User == null) {
                throw new LoginFailedException("Failed to authenticate user");
            }

            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");
            String provider = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId().toUpperCase();
            if (email == null)
                throw new LoginFailedException("Failed to fetch email from " + provider);
            if (name == null)
                name = email.split("@")[0];

            //if user email is not registered follow signup
            if (!userRepo.existsByEmail(email)) {
                OauthSignupRequest signupRequest = new OauthSignupRequest(email, provider, name);
                oauthService.signupUser(signupRequest);
            }
            String jwtToken = jwtService.generateToken(email);
            response = oauthService.setJwtCookieAndHeader(request, response, jwtToken);

            response.sendRedirect(redirectUrl);
        }
        catch(LoginFailedException e)
        {
            handlerExceptionResolver.resolveException(request,response,null,e);
        }
    }

}
