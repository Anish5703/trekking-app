package com.example.trekking_app.service;

import com.example.trekking_app.dto.oauth.OauthSignupRequest;
import com.example.trekking_app.entity.OauthUser;
import com.example.trekking_app.exception.auth.DuplicateEmailFoundException;
import com.example.trekking_app.exception.auth.SignupFailedException;
import com.example.trekking_app.mapper.OauthUserMapper;
import com.example.trekking_app.repository.OauthUserRepository;
import com.example.trekking_app.repository.UserRepository;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class OauthService {

    private final OauthUserRepository oauthRepo;
    private final UserRepository userRepo;
    private final OauthUserMapper oauthUserMapper;

    public OauthService(OauthUserRepository oauthRepo, UserRepository userRepo)
    {
        this.oauthRepo = oauthRepo;
        this.userRepo = userRepo;
        this.oauthUserMapper = new OauthUserMapper();
    }


    public HttpServletResponse setJwtCookieAndHeader(HttpServletRequest servletRequest, HttpServletResponse servletResponse, String jwtToken)
    {
        boolean isProduction = !servletRequest.getServerName().equals("localhost");
        Cookie cookie = new Cookie("jwt",jwtToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction);
        cookie.setPath("/");
        cookie.setMaxAge(1200);
        servletResponse.addCookie(cookie);
        servletResponse.addHeader("Authorization" ,String.format("Bearer %s",jwtToken));
        return servletResponse;
    }

    public void signupUser(OauthSignupRequest signupRequest)
    {
        try {
            if (userRepo.existsByEmail(signupRequest.getEmail()))
                throw new DuplicateEmailFoundException("User already registered with this email");
            OauthUser oauthUser = oauthUserMapper.toOauthUser(signupRequest);
            oauthRepo.save(oauthUser);
        }
        catch (Exception e)
        {
            log.error("Oauth signup failed : {}",e.getLocalizedMessage());
            throw new SignupFailedException("Failed to signup user");
        }
    }
}

