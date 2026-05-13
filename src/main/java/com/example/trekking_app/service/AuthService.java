package com.example.trekking_app.service;

import com.example.trekking_app.dto.auth.*;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.entity.OauthUser;
import com.example.trekking_app.entity.Token;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.exception.auth.*;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.resource.ResourceUpdateFailedException;
import com.example.trekking_app.mapper.TokenMapper;
import com.example.trekking_app.mapper.UserMapper;
import com.example.trekking_app.model.Role;
import com.example.trekking_app.repository.OauthUserRepository;
import com.example.trekking_app.repository.TokenRepository;
import com.example.trekking_app.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepo;
    private final TokenRepository tokenRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final OauthUserRepository oauthUserRepo;
    private final TokenService tokenService;
    private final UserMapper userMapper = new UserMapper();
    private final TokenMapper tokenMapper = new TokenMapper();

    /*
    * Signup User flow
    * Validate SignupRequest dto for empty fields and email duplication
    * If email exists but not verified resends confirmation token and returns mail resend response
    * Map SignupRequest dto to User entity
    * Encode raw password
    * Save user in user specific repository referring the role
    * Send Confirmation link to the user
    * Return ApiResponse<SignupResponse> dto
    */

    @Transactional
    public ApiResponse<SignupResponse> signupUser(@NonNull SignupRequest request, HttpServletRequest servletRequest)
    {
       ApiResponse<SignupResponse> response = this.validateSignupRequest(request,servletRequest);
       if(response!=null)
           return response;

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
       User newUser = userRepo.save(user);
        if (newUser == null)
            throw new SignupFailedException("Failed to save new user : "+newUser.getName());
        else {
            mailService.sendSignupConfirmationMail(newUser);
            SignupResponse signupResponse =userMapper.toSignupResponse(newUser);
            return new ApiResponse<>(signupResponse,"Check inbox for confirmation link",201);
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
    public ApiResponse<SignupResponse> resendSignupConfirmation(String email)
    {

            User user = userRepo.findByEmail(email)
                    .orElseThrow( () -> new UserNotFoundException("Email not registered fill the signup form"));
            if(user.isEmailVerified())
                throw new EmailAlreadyVerifiedException("Email is already verified.You can go to login page");
            try{
                tokenRepo.deleteByUser_Email(email);
                mailService.sendSignupConfirmationMail(user);
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
         if(user.getRole().equals(Role.CUSTOMER)) user.setActive(true);
         else if(user.getRole().equals(Role.ADMIN)) user.setActive(false);
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
    public ApiResponse<SignupResponse> validateSignupRequest(SignupRequest request,HttpServletRequest servletRequest)
    {

            if (request.getName().isEmpty() || request.getEmail().isEmpty() || request.getPassword().isEmpty())
                throw new EmptySignupFieldException("Signup fields cannot be empty");
            if (userRepo.existsByEmail(request.getEmail()))
            {
                Optional<User> user = userRepo.findByEmail(request.getEmail());

                if(user.isPresent())
                {
                    if (!user.get().isEmailVerified())
                       return resendSignupConfirmation(user.get().getEmail());
                    else
                    {
                log.error("User with email {} already exists", request.getEmail());
                throw new DuplicateEmailFoundException("Email already exists");
                }
            }
    }
            return null;
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
            if(!user.isActive() && user.getRole().equals(Role.ADMIN)) throw new LoginFailedException("new admin account must be approved by any existing admin ");
            if(!user.isActive() && user.getRole().equals(Role.CUSTOMER)) throw new LoginFailedException("account is blocked by admin");
            if(oauthUserRepo.existsByEmail(request.getEmail()))
            {
                Optional<OauthUser> oauthUser = oauthUserRepo.findByEmail(request.getEmail());
                if(oauthUser.isPresent()) throw new LoginFailedException("Login with "+oauthUser.get().getProvider());
            }
            Authentication auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword()));
            String message = "Login Successful";

            String jwtToken = jwtService.generateAccessToken(request.getEmail());
            String refreshToken = tokenService.generateRefreshToken(user);

            LoginResponse loginResponse = userMapper.toLoginResponse(user);

            loginResponse.setAccessToken(jwtToken);
            loginResponse.setRefreshToken(refreshToken);

            return new ApiResponse<>(loginResponse,message,200);
        }
        catch(AuthenticationException ex)
        {
            throw new LoginFailedException("Credentials didn't matched");
        }
    }


    //Method to reset user password
    @Transactional
    public ApiResponse<PasswordResetResponse> passwordReset(@NonNull PasswordResetRequest resetRequest , @NonNull Integer userId )
    {
        if(resetRequest.getOldPassword().equals(resetRequest.getNewPassword()))
            throw new ResourceUpdateFailedException("new password must be different from old password");

        User user = userRepo.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("user" , "id" , userId)
        );
        if(!passwordEncoder.matches(resetRequest.getOldPassword(),user.getPassword()))
            throw new ResourceUpdateFailedException("failed to update password ! old password is incorrect");
        user.setPassword(passwordEncoder.encode(resetRequest.getNewPassword()));
        User modifiedUser = userRepo.save(user);
        PasswordResetResponse resetResponse = PasswordResetResponse.builder()
                .userId(modifiedUser.getId())
                .refreshToken(jwtService.generateRefreshToken(modifiedUser.getEmail()))
                .accessToken(jwtService.generateAccessToken(modifiedUser.getEmail()))
                .build();

        return new ApiResponse<>(resetResponse,"password updated",200);
    }

    @Transactional
    public ApiResponse<Void> forgotPasswordReset(@NonNull String email)
    {
        User user = userRepo.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("user","email",email)
        );
        Optional<Token> token = tokenRepo.findByUser(user);
        token.ifPresent(tokenRepo::delete);
        mailService.sendForgotPasswordResetMail(user);
        return new ApiResponse<>(null,"Check inbox for password reset confirmation link ! link will be expired in 5 minutes",200);

    }

    @Transactional
    public ApiResponse<ForgotPasswordResetResponse> VerifyForgotPasswordReset(@NonNull ForgotPasswordResetRequest resetPasswordRequest)
    {
      if(resetPasswordRequest.getToken().isBlank() || resetPasswordRequest.getNewPassword().isBlank())
          throw new IllegalArgumentException("token and new password required");

      Token token = tokenRepo.findByTokenName(resetPasswordRequest.getToken()).orElseThrow(
              () -> new ResourceNotFoundException("token","name",resetPasswordRequest.getToken())
      );
      User user = userRepo.findById(token.getUser().getId()).orElseThrow(
              () -> new ResourceNotFoundException("user","email",token.getUser().getEmail())
      );
      user.setPassword(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
      userRepo.save(user);
      //Invalidating token after use
        token.setExpiryAt(LocalDateTime.now().plusMinutes(10));
        tokenRepo.save(token);

      ForgotPasswordResetResponse resetResponse = ForgotPasswordResetResponse.builder().email(user.getEmail()).build();
      return new ApiResponse<>(resetResponse,"password reset successfully",200);
    }

}
