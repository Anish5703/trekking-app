package com.example.trekking_app.controller;

import com.example.trekking_app.dto.auth.SignupRequest;
import com.example.trekking_app.dto.auth.SignupResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> handleSignupUser(@Valid @RequestBody SignupRequest signupRequest,
                                                                        HttpServletRequest servletRequest) {
        ApiResponse<SignupResponse> response = authService.signupUser(signupRequest, servletRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/signup/confirmation")
    public ResponseEntity<ApiResponse<SignupResponse>> handleSignupConfirmation(@RequestParam(name = "token") String token) {
        ApiResponse<SignupResponse> response = authService.validateSignupConfirmationToken(token);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type","application/json");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PutMapping("/signup/resend")
    public ResponseEntity<ApiResponse<SignupResponse>> handleResendSignupConfirmation(@RequestParam(name="email") String email,
                                                                                      HttpServletRequest servletRequest)
    {
        ApiResponse<SignupResponse> response = authService.resendSignupConfirmation(email, servletRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type","application/json");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }



}
