package com.example.trekking_app.service;

import com.example.trekking_app.dto.auth.LoginRequest;
import com.example.trekking_app.dto.auth.LoginResponse;
import com.example.trekking_app.dto.auth.SignupRequest;
import com.example.trekking_app.dto.auth.SignupResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.entity.OauthUser;
import com.example.trekking_app.entity.Token;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.exception.auth.*;
import com.example.trekking_app.mapper.UserMapper;
import com.example.trekking_app.repository.OauthUserRepository;
import com.example.trekking_app.repository.TokenRepository;
import com.example.trekking_app.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class AuthService {
    private final UserRepository userRepo;
    private final TokenRepository tokenRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final MailService mailService;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final OauthUserRepository oauthUserRepo;
    public AuthService(UserRepository userRepo,TokenRepository tokenRepo,
                       BCryptPasswordEncoder passwordEncoder,
                       MailService mailService,AuthenticationManager authManager,
                       JwtService jwtService,OauthUserRepository oauthUserRepo)
    {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.userMapper = new UserMapper();
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.oauthUserRepo = oauthUserRepo;
    }


    /*
    * Signup User flow
    * Validate SignupRequest dto for empty fields and email duplication
    * Map SignupRequest dto to User entity
    * Encode raw password
    * Save user in user specific repository referring the role
    * Send Confirmation link to the user
    * Return ApiResponse<SignupResponse> dto
    */

    @Transactional
    public ApiResponse<SignupResponse> signupUser(SignupRequest request, HttpServletRequest servletRequest)
    { try {
        this.validateSignupRequest(request,servletRequest);
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
       User newUser = userRepo.save(user);
        if (newUser == null)
            throw new SignupFailedException("Failed to save new user : "+newUser.getName());
        else {
            sendSignupConfirmationToken(newUser,servletRequest);
            SignupResponse signupResponse =userMapper.toSignupResponse(newUser);
            return new ApiResponse<>(signupResponse,"Check mail for confirmation link",201);
        }
    }
    catch(Exception e)
    {
        log.error("Database Exception : {}",e.getLocalizedMessage());
        throw new SignupFailedException("Failed to save new user");
    }
    }
    /*
    * Method resend signup confirmation link
    * Validate if email exists in User Repository and retrieve user
    * Validate if user is already verified
    * Delete previously generated token
    * Send confirmation link
    * return ApiResponse<SignupResponse> dto
    */
    @Transactional
    public ApiResponse<SignupResponse> resendSignupConfirmation(String email, HttpServletRequest servletRequest)
    {

            User user = userRepo.findByEmail(email)
                    .orElseThrow( () -> new UserNotFoundException("Email not registered fill the signup form"));
            if(user.isEmailVerified())
                throw new EmailAlreadyVerifiedException("Email is already verified.You can go to login page");
            tokenRepo.deleteByUserEmail(email);
            try{
            sendSignupConfirmationToken(user,servletRequest);
            String message = "Check mail for confirmation link";
            SignupResponse signupResponse = userMapper.toSignupResponse(user);
            return new ApiResponse<>(signupResponse,message,200);
        }
        catch(Exception e)
        {
            throw new SignupFailedException("Failed to send confirmation link");
        }
    }

    /*
    * Method to validate signup confirmation token
    * Validate if token exists in the repository
    * Retrieve user from token
    * Set user field emailVerified to true
    * Save updated user in the repository
    * Return ApiResponse<SignupResponse> dto
     */
 @Transactional
    public ApiResponse<SignupResponse> validateSignupConfirmationToken(String tokenName)
    {
     try
     {
         Token token = tokenRepo.findByTokenName(tokenName)
                 .orElseThrow(() -> new SignupFailedException("Invalid signup token"));
         User user = token.getUser();
         user.setEmailVerified(true);
         userRepo.save(user);
         SignupResponse signupResponse = userMapper.toSignupResponse(user);
         String message = "Email verified. You can now log in";
         return new ApiResponse<>(signupResponse,message,200);
     }
     catch(Exception e)
     {
         throw new SignupFailedException("Failed to validate signup token");
     }
    }
    /*
    * Method for validating SignupRequest dto for empty fields and duplicate email
    * throws exception if violates any terms else returns void
     */
    public void validateSignupRequest(SignupRequest request,HttpServletRequest servletRequest)
    {
        try {
            if (request.getName().isEmpty() || request.getEmail().isEmpty() || request.getPassword().isEmpty())
                throw new EmptySignupFieldException("Signup fields cannot be empty");

            if (userRepo.existsByEmail(request.getEmail()))
            {
                Optional<User> user = userRepo.findByEmail(request.getEmail());

                if(user.isPresent() && !user.get().isEmailVerified())
                  resendSignupConfirmation(user.get().getEmail(), servletRequest);
                log.error("User with email {} already exists", request.getEmail());
                throw new DuplicateEmailFoundException("Email already exists");
            }
        }
        catch(Exception e)
        {
            throw new SignupFailedException("Failed to save new user");
        }
    }

  /*
  * Method for user login
  * Validate if email is registered
  * Validate if email is verified
  * Check if user is created using oauth signin
  * Authenticate login credentials
  * Generate jwt token
  * Prepare LoginResponse
  * Return ApiResponse<LoginResponse> dto
   */
    @Transactional
    public ApiResponse<LoginResponse> loginUser(LoginRequest request)
    {
        try{
            User user = userRepo.findByEmail(request.getEmail())
                    .orElseThrow(
                            () -> new UserNotFoundException("Email not found")
                    );
            if(!user.isEmailVerified())
                throw new EmailNotVerifiedException("Email not verified. Check email inbox for confirmation or apply resend confirmation");
            if(oauthUserRepo.existsByEmail(request.getEmail()))
            {
                Optional<OauthUser> oauthUser = oauthUserRepo.findByEmail(request.getEmail());
                if(oauthUser.isPresent()) throw new LoginFailedException("Login with "+oauthUser.get().getProvider());
            }
            Authentication auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword()));
            String message = "Login Successful";
            String jwtToken = jwtService.generateToken(request.getEmail());
            LoginResponse loginResponse = userMapper.toLoginResponse(user);
            loginResponse.setJwtToken(jwtToken);
            return new ApiResponse<>(loginResponse,message,200);
        }
        catch(AuthenticationException ex)
        {
            throw new LoginFailedException("Credentials didn't matched");
        }
    }

    //Method to send registration confirmation token
    public void sendSignupConfirmationToken(User user, HttpServletRequest servletRequest) throws MessagingException
    {
        //generating token and storing it to the repo with username
        String tokenName = mailService.generateToken();
        log.info("Generated token {} for user {}",tokenName,user);
        Token token = new Token(tokenName,user);
        tokenRepo.save(token);

        //Concatenating url and token
        String confirmationLink = mailService.getConfirmationUrl(servletRequest)+token.getTokenName();

        //sending confirmation mail to the user
        String htmlContent = mailService.buildConfirmationEmail(user.getName(),confirmationLink);
        mailService.sendHtmlMail(user.getEmail(),"Confirmation Mail",htmlContent);
    }
}
