package com.example.trekking_app.service;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.oauth.OauthLoginRequest;
import com.example.trekking_app.dto.oauth.OauthSignupRequest;
import com.example.trekking_app.dto.oauth.OauthLoginResponse;
import com.example.trekking_app.dto.oauth.OauthUserInfo;
import com.example.trekking_app.entity.OauthUser;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.exception.auth.DuplicateEmailFoundException;
import com.example.trekking_app.exception.auth.LoginFailedException;
import com.example.trekking_app.exception.auth.SignupFailedException;
import com.example.trekking_app.mapper.OauthUserMapper;
import com.example.trekking_app.model.UserPrincipal;
import com.example.trekking_app.repository.OauthUserRepository;
import com.example.trekking_app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OauthService {

    private final OauthUserRepository oauthUserRepo;
    private final UserRepository userRepo;
    private final OauthUserMapper oauthUserMapper = new OauthUserMapper();
    private final JwtService jwtService;
    private final GoogleOauthProvider googleOauthProvider;
    private final FacebookOauthProvider facebookOauthProvider;
    private final TokenService tokenService;

    @PersistenceContext
    private EntityManager entityManager;


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
            oauthUserRepo.save(oauthUser);
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
                oauthUser = oauthUserRepo.findByEmail(email)
                        .orElseThrow(
                                () -> new LoginFailedException("No user found with this email")
                        );
                OauthLoginResponse oauthLoginResponse = oauthUserMapper.toOauthLoginResponse(oauthUser);
                oauthLoginResponse.setAccessToken(jwtService.generateAccessToken(oauthLoginResponse.getEmail()));
                oauthLoginResponse.setRefreshToken(tokenService.generateRefreshToken(oauthUser));
                return new ApiResponse<>(oauthLoginResponse, "Login Successful", 200);
            }


        } catch (Exception e) {
            throw new LoginFailedException("Failed to authenticate user");
        }
    }

    @Transactional
    public ApiResponse<OauthLoginResponse> getAppOauthLogin(@NonNull OauthLoginRequest loginRequest)
    {
        if(loginRequest.getToken().isEmpty() || loginRequest.getProvider().isEmpty()) throw new IllegalArgumentException("token and provider field required");

        OauthUserInfo userInfo;

        switch(loginRequest.getProvider().toUpperCase())
        {
            case "GOOGLE" -> userInfo = googleOauthProvider.verify(loginRequest.getToken());
            case "FACEBOOK" -> userInfo = facebookOauthProvider.verify(loginRequest.getToken());
            default -> throw new IllegalArgumentException("Unknown oauth provider : " + loginRequest.getProvider());
        }
       try {
           boolean isExistingUser = oauthUserRepo.existsByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId());
           userInfo.setRole(loginRequest.getRole());
           OauthUser oauthUser = findOrCreateUser(userInfo);
           OauthLoginResponse loginResponse = oauthUserMapper.toOauthLoginResponse(oauthUser);
           loginResponse.setAccessToken(jwtService.generateAccessToken(loginResponse.getEmail()));
           loginResponse.setRefreshToken(tokenService.generateRefreshToken(oauthUser));
           String message = isExistingUser ? "login successful" : "signup successful";
           return new ApiResponse<>(loginResponse, message, 200);
       }
       catch (Exception e)
       {
           log.error("App Oauth failed : {}",e.getLocalizedMessage());
           throw new LoginFailedException("Login failed for "+userInfo.getEmail());
       }

    }

    @Transactional
    public OauthUser findOrCreateUser(@NonNull OauthUserInfo userInfo)
    {
        Optional<OauthUser> existingOauthUser = oauthUserRepo.findByProviderAndProviderId(userInfo.getProvider(),userInfo.getProviderId());

                if(existingOauthUser.isPresent())
                    return existingOauthUser.get();

                Optional<User> existingUser = userRepo.findByEmail(userInfo.getEmail());

                if(existingUser.isPresent())
                {
                   return  linkOauthProvider(existingUser.get(), userInfo);
                }
                else
                {
                    OauthUser oauthUser = oauthUserMapper.toOauthUser(userInfo);
                   return  oauthUserRepo.save(oauthUser);
                }

    }

    @Transactional
    public  OauthUser linkOauthProvider(User user , OauthUserInfo userInfo)
    {
        OauthUser oauthUser = oauthUserMapper.toOauthUser(user , userInfo);
        oauthUser.setId(user.getId());
        return entityManager.merge(oauthUser);

    }

}

