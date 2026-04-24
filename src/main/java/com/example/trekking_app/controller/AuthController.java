package com.example.trekking_app.controller;

import com.example.trekking_app.dto.auth.LoginRequest;
import com.example.trekking_app.dto.auth.LoginResponse;
import com.example.trekking_app.dto.auth.SignupRequest;
import com.example.trekking_app.dto.auth.SignupResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Signup User",
            description = "Start signup process using SignupRequest dto"
             )
    @ApiResponses(
            value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",description = "Check mail for confirmation link"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Signup failed")
            }
    )
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> handleUserSignup(@Valid @RequestBody SignupRequest signupRequest,
                                                                        HttpServletRequest servletRequest) {
        ApiResponse<SignupResponse> response = authService.signupUser(signupRequest, servletRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        return ResponseEntity.status(HttpStatus.CREATED).headers(headers).body(response);
    }
    @Operation(
            summary = "Validate confirmation token and set User property emailVerified to true ",
            description = "User clicks the confirmation link provided in inbox"
    )
    @ApiResponses(
            value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",description = "Email verified go to login page "),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Failed to validate signup token")
            }
    )
    @GetMapping("/signup/confirmation")
    public ResponseEntity<ApiResponse<SignupResponse>> handleSignupConfirmation(@RequestParam(name = "token") String token) {
        ApiResponse<SignupResponse> response = authService.validateSignupConfirmationToken(token);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type","application/json");
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);
    }
    @Operation(
            summary = "Resend signup confirmation link",
            description = "Email is send as query param to generate new validation token and resend signup confirmation link"
    )
    @ApiResponses(
            value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",description = "Check mail for confirmation link "),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Failed to send confirmation link")
            })

    @PutMapping("/signup/resend")
    public ResponseEntity<ApiResponse<SignupResponse>> handleResendSignupConfirmation(@RequestParam(name="email") String email,
                                                                                      HttpServletRequest servletRequest)
    {
        ApiResponse<SignupResponse> response = authService.resendSignupConfirmation(email, servletRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type","application/json");
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);
    }
    @Operation(
            summary = "Login User",
            description = " Start login process using LoginRequest dto"
    )
    @ApiResponses(
            value = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",description = "Login Successful"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Credentials didn't matched")
            })

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> handleUserLogin(@Valid @RequestBody LoginRequest loginRequest)
    {
        ApiResponse<LoginResponse> response = authService.loginUser(loginRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type","application/json");
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);
    }

}
