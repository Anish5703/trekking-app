package com.example.trekking_app.service.auth;

import com.example.trekking_app.config.IpExtractor;
import com.example.trekking_app.dto.auth.*;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.user.UserDetails;
import com.example.trekking_app.entity.OauthUser;
import com.example.trekking_app.entity.Token;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.exception.auth.*;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.resource.ResourceUpdateFailedException;
import com.example.trekking_app.model.ClientType;
import com.example.trekking_app.model.Role;
import com.example.trekking_app.repository.OauthUserRepository;
import com.example.trekking_app.repository.TokenRepository;
import com.example.trekking_app.repository.UserRepository;
import com.example.trekking_app.service.security.BruteForceProtectionService;
import com.example.trekking_app.service.user.MailService;
import com.example.trekking_app.service.user.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    //signup test= basic signup request object sent ,encode password
    private  User user;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private TokenService tokenService;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private MailService mailService;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private OauthUserRepository oauthUserRepository;
    @Mock
    private BruteForceProtectionService bruteForce;
    @Mock
    private IpExtractor ipExtractor;

    @InjectMocks
    private  AuthService authService;

    @BeforeEach
    void setUp(){
        user = new User();

        HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
        ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(mockHttpServletRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
    }

    @Test
    void shouldSignupUserSuccessfully() {
        SignupRequest request = new SignupRequest();
        request.setName("Test");
        request.setEmail("test@email.com");
        request.setPassword("12345");
        request.setContact("9810222341");
        request.setRole(Role.CUSTOMER);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("12345")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ApiResponse<SignupResponse> response = authService.signupUser(request, ClientType.WEB);


        assertNotNull(response);
        assertEquals(201,response.getStatus());
    }


    // validateSignupRequest
    @Test
    void shouldThrowExceptionWhenSignUpFails(){
        SignupRequest request = new SignupRequest();
        request.setName("");
        request.setEmail("");
        request.setPassword("");
        assertThrows(EmptySignupFieldException.class,()->authService.validateSignupRequest(request,ClientType.WEB));
    }

    //validateSignupRequest
    @Test
    void shouldThrowDuplicateEmailFoundException(){
        String email = "test@email.com";
        SignupRequest request = new SignupRequest();
        request.setName("test");
        request.setEmail(email);
        request.setPassword("12345");
        user.setEmailVerified(true);
        when(userRepository.existsByEmail(email)).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
       assertThrows(DuplicateEmailFoundException.class,()->authService.signupUser(request,ClientType.WEB));
    }

    //validateSignupRequest=email exists but not verified
    @Test
    void shouldCallResendSignupConfirmation(){
        String email="test@email.com";
        SignupRequest request = new SignupRequest();
        request.setName("test");
        request.setEmail(email);
        request.setPassword("12345");
        user.setEmail(email);
        user.setEmailVerified(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        ApiResponse<SignupResponse> response = authService.signupUser(request,ClientType.WEB);

        assertNotNull(response);
        assertEquals(200,response.getStatus());
        verify(tokenRepository).deleteByUser_Email(email);
        verify(mailService).sendSignupConfirmationMail(user,ClientType.WEB);

    }


    @Test
    void validatedSignUpConfirmationTokenSuccessfully() {
        Token token = new Token();
        token.setTokenName("token");
        token.setUser(user);
        user.setEmailVerified(true);
        user.setRole(Role.CUSTOMER);
        user.setActive(false);
        when(tokenRepository.findByTokenName(token.getTokenName())).thenReturn(Optional.of(token));

        ApiResponse<SignupResponse> response = authService.validateSignupConfirmationToken(token.getTokenName());

        assertNotNull(response);
        assertTrue(user.isActive());
        assertEquals(200, response.getStatus());
        assertTrue(user.isEmailVerified());
        assertTrue(user.isActive());
    }

    @Test
    void resentSignUpConfirmationSuccessfully(){

        user.setEmail("test@email.com");
        user.setEmailVerified(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        ApiResponse<SignupResponse> response = authService.resendSignupConfirmation(user.getEmail(),ClientType.WEB);
        assertNotNull(response);
        assertEquals(200,response.getStatus());
        verify(tokenRepository).deleteByUser_Email(user.getEmail());
        verify(mailService).sendSignupConfirmationMail(user,ClientType.WEB);
    }

    @Test
    void userLoginSuccessfull(){
        user.setEmail("test@email.com");
        OauthUser oauthUser = new OauthUser();
        oauthUser.setPassword("12345");
        user.setEmailVerified(true);
        user.setActive(true);
        user.setRole(Role.CUSTOMER);
        LoginRequest request = new LoginRequest();
        request.setEmail(user.getEmail());
        request.setPassword("12345");

        when(ipExtractor.extract(any())).thenReturn("127.0.0.1");
        when(bruteForce.isBlocked("127.0.0.1")).thenReturn(false);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(oauthUserRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(authManager.authenticate(any(Authentication.class))).thenReturn(mock(Authentication.class));
        when(jwtService.generateAccessToken((user.getEmail()))).thenReturn("token");
        when(tokenService.generateRefreshToken(user)).thenReturn("refreshToken");

        ApiResponse<LoginResponse> response = authService.loginUser(request);
        assertNotNull(response);
        assertEquals("token",response.getData().getAccessToken());
        assertEquals("refreshToken",response.getData().getRefreshToken());
        assertEquals(200,response.getStatus());

    }

    @Test
    void testLoginUser_AccountLockedException(){
        user.setEmail("test@email.com");
        LoginRequest req = new LoginRequest();
        req.setEmail(user.getEmail());
        req.setPassword("12345");

        when(ipExtractor.extract(any())).thenReturn("127.0.0.1");
        when(bruteForce.isBlocked("127.0.0.1")).thenReturn(true);

        assertThrows(AccountLockedException.class,()->authService.loginUser(req));
    }

    @Test
    void testLoginUser_UserNotFound() {
        user.setEmail("test@email.com");
        LoginRequest req = new LoginRequest();
        req.setEmail(user.getEmail());
        req.setPassword("12345");

        when(ipExtractor.extract(any())).thenReturn("127.0.0.1");
        when(bruteForce.isBlocked("127.0.0.1")).thenReturn(false);
        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,()->authService.loginUser(req));
    }

    @Test
    void shouldThrowEmailNotVerifiedException(){
        user.setEmail("test@email.com");
        user.setEmailVerified(false);
        user.setRole(Role.CUSTOMER);
        LoginRequest request = new LoginRequest();
        request.setEmail(user.getEmail());
        request.setPassword("12345");

        when(ipExtractor.extract(any())).thenReturn("127.0.0.1");
        when(bruteForce.isBlocked("127.0.0.1")).thenReturn(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        assertThrows(EmailNotVerifiedException.class,()->authService.loginUser(request));
    }

    @Test
    void shouldThrowLoginFailedExceptionWhenUserIsInActive(){
        user.setEmail("test@email.com");
        user.setEmailVerified(true);
        user.setRole(Role.CUSTOMER);
        user.setActive(false);
        LoginRequest request = new LoginRequest();
        request.setEmail(user.getEmail());
        request.setPassword("12345");
        when(ipExtractor.extract(any())).thenReturn("127.0.0.1");
        when(bruteForce.isBlocked("127.0.0.1")).thenReturn(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        assertThrows(LoginFailedException.class,()->authService.loginUser(request));

    }
    @Test
    void shouldThrowResourceUpdateFailedException(){
        PasswordResetRequest request = new PasswordResetRequest();
        Integer id = 10;
        request.setOldPassword("12345");
        request.setNewPassword("12345");
        assertThrows(ResourceUpdateFailedException.class,()->authService.passwordReset(request,id));
    }

    @Test
    void testPasswordReset_ResourceNotFound() {
        PasswordResetRequest req = new PasswordResetRequest();
        req.setOldPassword("12345");
        req.setNewPassword("123455");
        Integer id = 10;

        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->authService.passwordReset(req,id));

    }

    @Test
    void testPasswordReset_IncorrectOldPassword(){
        PasswordResetRequest req = new PasswordResetRequest();
        req.setOldPassword("12345");
        req.setNewPassword("123455");
        Integer id = 10;

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.getOldPassword(),user.getPassword())).thenReturn(false);
        assertThrows(ResourceUpdateFailedException.class,()->authService.passwordReset(req,id));
    }

    @Test
    void passwordResetSuccessfully(){

        PasswordResetRequest request = new PasswordResetRequest();
        Integer userId=1;
        request.setOldPassword("12345");
        request.setNewPassword("54321");
        user.setEmail("test@email.com");
        user.setPassword("12345");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getOldPassword(),user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation->invocation.getArgument(0));
        when(jwtService.generateAccessToken(anyString())).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(anyString())).thenReturn("refreshToken");

        ApiResponse<PasswordResetResponse> response = authService.passwordReset(request,userId);
        assertNotNull(response);
        assertEquals(200,response.getStatus());
        assertEquals("accessToken",response.getData().getAccessToken());
        assertEquals("refreshToken",response.getData().getRefreshToken());
    }

    @Test
    void forgetPasswordResetSuccess(){
      user.setEmail("test@email.com");
      user.setEmailVerified(true);
      Token token = new Token();

      when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
      when(tokenRepository.findByUser(user)).thenReturn(Optional.of(token));
      ApiResponse<Void> response = authService.forgotPasswordReset(user.getEmail(),ClientType.WEB);
      assertNotNull(response);

      verify(tokenRepository).delete(token);
      verify(mailService).sendForgotPasswordResetMail(user,ClientType.WEB);
    }

    //forgot password Reset
    @Test
    void shouldThrowResourceNotFoundException(){
        user.setEmail("test@email.com");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->authService.forgotPasswordReset(user.getEmail(),ClientType.WEB));
    }

    @Test
    void ThrowEmailNotVerifiedExceptionInForgotPasswordResetMethod(){
        user.setEmail("test@email.com");
        user.setEmailVerified(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        assertThrows(EmailNotVerifiedException.class,()->authService.forgotPasswordReset(user.getEmail(), ClientType.WEB));
    }


    @Test
    void shouldVerifyForgotPasswordResetSuccessfully() {

        user.setId(1);
        user.setEmail("test@email.com");
        Token token = new Token();
        token.setTokenName("abc123");
        token.setUser(user);

        ForgotPasswordResetRequest request = new ForgotPasswordResetRequest();

        request.setToken("abc123");
        request.setNewPassword("newPassword");

        when(tokenRepository.findByTokenName("abc123")).thenReturn(Optional.of(token));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedPassword");

        ApiResponse<ForgotPasswordResetResponse> response = authService.VerifyForgotPasswordReset(request);

        assertNotNull(response);
        assertEquals(200, response.getStatus());

        assertEquals("encodedPassword", user.getPassword());

        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void shouldThrowIllegalArgumentException(){
        ForgotPasswordResetRequest request = new ForgotPasswordResetRequest();
        request.setToken("");
        request.setNewPassword("");
        assertThrows(IllegalArgumentException.class,()->authService.VerifyForgotPasswordReset(request));

    }

    @Test
    void shouldThrowResourceNotFoundExceptionInVerifyForgotPasswordResetMethod(){
        ForgotPasswordResetRequest request = new ForgotPasswordResetRequest();
        request.setToken("token");
        request.setNewPassword("12345");
        Token token = new Token();
        when(tokenRepository.findByTokenName(request.getToken())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->authService.VerifyForgotPasswordReset(request));
    }

    @Test
    void testVerifyForgotPasswordReset_UserResourceNotFound(){
        ForgotPasswordResetRequest request = new ForgotPasswordResetRequest();
        request.setToken("token");
        request.setNewPassword("12345");

        user.setId(1);
        user.setEmail("test@email.com");

        Token token = new Token();
        token.setTokenName("abc123");
        token.setUser(user);

        when(tokenRepository.findByTokenName(request.getToken())).thenReturn(Optional.of(token));
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,()->authService.VerifyForgotPasswordReset(request));
    }

    @Test
    void testGetUserInfo_Success(){
        Integer id = 1;
        user.setName("user");
        user.setEmail("test@email.com");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        ApiResponse<UserDetails> response = authService.getUserInfo(id);
        assertNotNull(response);
        assertEquals(200,response.getStatus());
        assertEquals("user",response.getData().getName());
        assertEquals("test@email.com",response.getData().getEmail());
    }

    @Test
    void testUserInfo_UserNotFound(){

        when(userRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,()->authService.getUserInfo(anyInt()));
    }
}


