package com.example.trekking_app.service.auth;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.oauth.OauthLoginRequest;
import com.example.trekking_app.dto.oauth.OauthLoginResponse;
import com.example.trekking_app.dto.oauth.OauthSignupRequest;
import com.example.trekking_app.dto.oauth.OauthUserInfo;
import com.example.trekking_app.entity.OauthUser;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.exception.auth.LoginFailedException;
import com.example.trekking_app.exception.auth.SignupFailedException;
import com.example.trekking_app.model.Role;
import com.example.trekking_app.model.UserPrincipal;
import com.example.trekking_app.repository.OauthUserRepository;
import com.example.trekking_app.repository.UserRepository;
import com.example.trekking_app.service.user.TokenService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OauthServiceTest {

    @InjectMocks
    private OauthService oauthService;
    private User user;

    @Mock
    private OauthUserRepository oauthUserRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private JwtService jwtService;
    @Mock
    private GoogleOauthProvider googleOauthProvider;
    @Mock
    private FacebookOauthProvider facebookOauthProvider;
    @Mock
    private TokenService tokenService;
    @Mock
    private Authentication authentication;
    @Mock
    private UserPrincipal userPrincipal;
    @Mock
    private EntityManager entityManager;

    @BeforeEach
    void setUp(){
        user = new User();
        user.setEmail("test@email.com");
        ReflectionTestUtils.setField(oauthService,"entityManager",entityManager);
    }

    @Test
    void validUserSignup() {
     OauthSignupRequest request = new OauthSignupRequest();
     request.setEmail(user.getEmail());
     request.setProvider("Facebook");
     request.setName("Test User");

     when(userRepo.existsByEmail(request.getEmail())).thenReturn(false);
     when(oauthUserRepo.save(any((OauthUser.class)))).thenAnswer(i->i.getArgument(0));
     oauthService.signupUser(request);
     ArgumentCaptor<OauthUser> captor = ArgumentCaptor.forClass(OauthUser.class);
     verify(oauthUserRepo).save(captor.capture());
     OauthUser saved = captor.getValue();

     assertEquals(user.getEmail(),saved.getEmail());
     assertEquals("Facebook",saved.getProvider());
     assertEquals("Test User", saved.getName());
     assertEquals(Role.CUSTOMER,saved.getRole());

    }

    //signup_request- email,provider,name
    @Test
    void shouldThrowSignupFailedExceptionDuringSignup() {
        OauthSignupRequest request= new OauthSignupRequest();
        request.setEmail(user.getEmail());
        when(userRepo.existsByEmail(request.getEmail())).thenReturn(true);
        assertThrows(SignupFailedException.class,()->oauthService.signupUser(request));
    }

    @Test
    void validOauthLoginResponse() {

        String email = user.getEmail();
        when(userPrincipal.getUsername()).thenReturn(email);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);

        OauthUser oauthUser = new OauthUser();
        oauthUser.setEmail(email);
        when(oauthUserRepo.findByEmail(email)).thenReturn(Optional.of(oauthUser));

        when(jwtService.generateAccessToken(email)).thenReturn("accessToken");
        when(tokenService.generateRefreshToken(oauthUser)).thenReturn("refreshToken");

        ApiResponse<OauthLoginResponse> response = oauthService.getOauthLogin(authentication);

        assertNotNull(response);
        assertEquals(200,response.getStatus());
        assertEquals("accessToken",response.getData().getAccessToken());
        assertEquals("refreshToken",response.getData().getRefreshToken());
    }

    @Test
    void shouldThrowLoginFailedExceptionDuringLoginWhenEmailIsEmpty(){
        String email ="";
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.getUsername()).thenReturn(email);
        assertThrows(LoginFailedException.class,()->oauthService.getOauthLogin(authentication));
    }

    @Test
    void shouldThrowLoginFailedExceptionWhenOauthUserNotFound() {
        String email = user.getEmail();
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userPrincipal.getUsername()).thenReturn(email);
        assertThrows(LoginFailedException.class,()->oauthService.getOauthLogin(authentication));
    }

    @Test
    void validFindOrCreateUserWhenOauthUserExists() {
        OauthUserInfo userInfo = new OauthUserInfo();
        userInfo.setProvider("Facebook");
        userInfo.setProviderId("123");
        userInfo.setEmail(user.getEmail());
        userInfo.setRole(Role.CUSTOMER);

        OauthUser existingUser = new OauthUser();
        existingUser.setProvider(userInfo.getProvider());
        existingUser.setProviderId(userInfo.getProviderId());
        when(oauthUserRepo.findByProviderAndProviderId(userInfo.getProvider(),userInfo.getProviderId())).thenReturn(Optional.of(existingUser));

       OauthUser response = oauthService.findOrCreateUser(userInfo);
       verify(oauthUserRepo).findByProviderAndProviderId(userInfo.getProvider(),userInfo.getProviderId());
        assertEquals(existingUser.getProvider(),response.getProvider());
        assertEquals("123",response.getProviderId());
        assertSame(existingUser,response);
    }

    @Test
    void validFindOrCreateUserWhenLocalUserExists(){
      OauthUserInfo info = new OauthUserInfo();
      info.setProvider("Facebook");
      info.setProviderId("123");
      info.setEmail(user.getEmail());

      OauthUser existingOauthUser = new OauthUser();
      existingOauthUser.setProviderId(info.getProviderId());
      existingOauthUser.setProvider(info.getProvider());
      when(oauthUserRepo.findByProviderAndProviderId(info.getProvider(),info.getProviderId())).thenReturn(Optional.empty());

      user.setId(1);
      user.setPassword("password");
      when(userRepo.findByEmail(info.getEmail())).thenReturn(Optional.of(user));
      when(entityManager.merge(any(OauthUser.class))).thenAnswer(i->i.getArgument(0));
      OauthUser response = oauthService.findOrCreateUser(info);
      verify(userRepo).findByEmail(info.getEmail());
      assertEquals(user.getId(),response.getId());
      assertEquals(existingOauthUser.getProvider(),response.getProvider());

    }

    @Test
    void validFindOrCreateUserCreatesCustomOauthUser(){
        OauthUserInfo info = new OauthUserInfo();
        info.setProvider("Facebook");
        info.setProviderId("123");
        info.setEmail(user.getEmail());
        info.setRole(Role.CUSTOMER);
        user.setEmailVerified(true);

        when(oauthUserRepo.findByProviderAndProviderId(info.getProvider(),info.getProviderId())).thenReturn(Optional.empty());
        when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(oauthUserRepo.save(any(OauthUser.class))).thenAnswer(i->i.getArgument(0));

        OauthUser response = oauthService.findOrCreateUser(info);
        assertNotNull(response);
        assertEquals(info.getRole(),response.getRole());
        verify(oauthUserRepo).save(any(OauthUser.class));
    }

    @Test
    void linkOauthProviderSuccess(){
        OauthUserInfo info = new OauthUserInfo();
        info.setProviderId("123");
        info.setProvider("Facebook");
        user.setId(1);
        user.setPassword("password");

        when(entityManager.merge(any(OauthUser.class))).thenAnswer(i->i.getArgument(0));
        OauthUser response = oauthService.linkOauthProvider(user,info);
        assertNotNull(response);
        assertEquals(user.getId(),response.getId());
    }
    // Get Appoauthlogin
    @Test
    void validGetAppOauthLoginWhenNewUser(){

        OauthLoginRequest req = new OauthLoginRequest();
        req.setId_token("token");
        req.setProvider("GOOGLE");
        req.setRole(Role.CUSTOMER);

        OauthUserInfo info = new OauthUserInfo();
        info.setProvider("GOOGLE");
        info.setProviderId("123");
        info.setEmail(user.getEmail());
        info.setRole(Role.CUSTOMER);

        when(googleOauthProvider.verify("token")).thenReturn(info);

        when(oauthUserRepo.existsByProviderAndProviderId("GOOGLE", "123")).thenReturn(false);
        when(oauthUserRepo.findByProviderAndProviderId(any(), any())).thenReturn(Optional.empty());
        when(userRepo.findByEmail(info.getEmail())).thenReturn(Optional.empty());
        when(oauthUserRepo.save(any(OauthUser.class))).thenAnswer(i->i.getArgument(0));

        when(jwtService.generateAccessToken(info.getEmail())).thenReturn("access");
        when(tokenService.generateRefreshToken(any())).thenReturn("refresh");

        ApiResponse<OauthLoginResponse> result = oauthService.getAppOauthLogin(req);

        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertEquals("access", result.getData().getAccessToken());
        assertEquals("refresh", result.getData().getRefreshToken());
        assertEquals("signup successful",result.getMessage());
        assertEquals("GOOGLE",result.getData().getProvider());

    }

    @Test
    void validGetAppOauthLoginWhenExistingUser(){
        OauthLoginRequest request = new OauthLoginRequest();
        request.setRole(Role.CUSTOMER);
        request.setProvider("GOOGLE");
        request.setId_token("token");

        OauthUserInfo info = new OauthUserInfo();
        info.setProviderId("123");
        info.setProvider("GOOGLE");

        OauthUser existingUser = new OauthUser();
        existingUser.setId(100);
        existingUser.setRole(Role.CUSTOMER);
        existingUser.setActive(true);
        existingUser.setProvider(info.getProvider());
        existingUser.setProviderId(info.getProviderId());
        existingUser.setEmail(user.getEmail());

        when(googleOauthProvider.verify(request.getId_token())).thenReturn(info);

        when(oauthUserRepo.existsByProviderAndProviderId(info.getProvider(),info.getProviderId())).thenReturn(true);
        when(oauthUserRepo.findByProviderAndProviderId(info.getProvider(),info.getProviderId())).thenReturn(Optional.of(existingUser));

        when(jwtService.generateAccessToken(existingUser.getEmail())).thenReturn("accessToken");
        when(tokenService.generateRefreshToken(any(OauthUser.class))).thenReturn("refreshToken");

        ApiResponse<OauthLoginResponse> result = oauthService.getAppOauthLogin(request);

        assertNotNull(result);
        assertEquals("accessToken",result.getData().getAccessToken());
        assertEquals("refreshToken",result.getData().getRefreshToken());
        assertEquals("login successful",result.getMessage());

    }

    @Test
    void shouldThrowIllegalArgumentExceptionDueToUnknownProvider(){

        OauthLoginRequest request = new OauthLoginRequest();
        request.setId_token("token");
        request.setProvider("github");

        assertThrows(IllegalArgumentException.class, ()->oauthService.getAppOauthLogin(request));

    }

    @Test
    void shouldThrowIllegalArgumentExceptionDueToInvalidRequest(){

        OauthLoginRequest request = new OauthLoginRequest();
        request.setId_token("");
        request.setProvider("");

        assertThrows(IllegalArgumentException.class,()->oauthService.getAppOauthLogin(request));
    }
}
