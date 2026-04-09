package com.example.trekking_app.service;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.oauth.OauthSignupRequest;
import com.example.trekking_app.dto.oauth.OauthLoginResponse;
import com.example.trekking_app.entity.OauthUser;
import com.example.trekking_app.exception.auth.DuplicateEmailFoundException;
import com.example.trekking_app.exception.auth.LoginFailedException;
import com.example.trekking_app.exception.auth.SignupFailedException;
import com.example.trekking_app.mapper.OauthUserMapper;
import com.example.trekking_app.model.UserPrincipal;
import com.example.trekking_app.repository.OauthUserRepository;
import com.example.trekking_app.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class OauthService {

    private final OauthUserRepository oauthRepo;
    private final UserRepository userRepo;
    private final OauthUserMapper oauthUserMapper;
    private final JwtService jwtService;

    public OauthService(OauthUserRepository oauthRepo, UserRepository userRepo,JwtService jwtService) {
        this.oauthRepo = oauthRepo;
        this.userRepo = userRepo;
        this.oauthUserMapper = new OauthUserMapper();
        this.jwtService = jwtService;
    }

    /*
    * Method set jwt token in cookie and set Authorization response header
    * Create Cookie and set attributes
    * Add Cookie to the response
    * Add Header Authorization with jwt token eg: "Bearer $jwtToken"
    * Return response;
     */

    public HttpServletResponse setJwtCookieAndHeader(HttpServletRequest servletRequest, HttpServletResponse servletResponse, String jwtToken) {
        boolean isProduction = !servletRequest.getServerName().equals("localhost");
        Cookie cookie = new Cookie("jwt", jwtToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(isProduction);
        cookie.setPath("/");
        cookie.setMaxAge(1200);
        servletResponse.addCookie(cookie);
        servletResponse.addHeader("Authorization", String.format("Bearer %s", jwtToken));
        return servletResponse;
    }

    /*
    * Method to signup user using oauth
    * Validate if email already exists
    * Prepare OauthUser
    * Save OauthUser to the OauthUserRepository
     */

    @Transactional
    public void signupUser(OauthSignupRequest signupRequest) {
        try {
            if (userRepo.existsByEmail(signupRequest.getEmail()))
                throw new DuplicateEmailFoundException("User already registered with this email");
            OauthUser oauthUser = oauthUserMapper.toOauthUser(signupRequest);
            oauthRepo.save(oauthUser);
        } catch (Exception e) {
            log.error("Oauth signup failed : {}", e.getLocalizedMessage());
            throw new SignupFailedException("Failed to signup user");
        }
    }

    /*
    * Method to login user using oauth
    * Fetch UserPrincipal from authentication object
    * Validate if UserPrincipal is present
    * Fetch email from UserPrincipal
    * Validate if email exists
    * Fetch OauthUser from OauthUserRepository
    * Prepare OauthLoginResponse
    * Return ApiResponse<OauthLoginResponse>
     */

    public ApiResponse<OauthLoginResponse> getOauthLogin(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            OauthUser oauthUser = null;
            if (userPrincipal == null)
                throw new LoginFailedException("Failed to fetch user credentials");
            String email = userPrincipal.getUsername();  //since username = email in UserPrincipal
                if (email.isEmpty())
                    throw new LoginFailedException("Failed to fetch user credentials");
                else {
                    oauthUser = oauthRepo.findByEmail(email)
                            .orElseThrow(
                                    () -> new LoginFailedException("No user found with this email")
                            );
                    OauthLoginResponse oauthLoginResponse = oauthUserMapper.toOauthUserDetails(oauthUser);
                    oauthLoginResponse.setJwtToken(jwtService.generateToken(oauthLoginResponse.getEmail()));
                    return new ApiResponse<>(oauthLoginResponse, "Login Successful", 200);
                }


        } catch (Exception e) {
            throw new LoginFailedException("Failed to authenticate user");
        }
    }
}

