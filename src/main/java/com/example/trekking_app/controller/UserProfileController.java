package com.example.trekking_app.controller;

import com.example.trekking_app.dto.auth.PasswordResetRequest;
import com.example.trekking_app.dto.auth.PasswordResetResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.user.UserDetails;
import com.example.trekking_app.model.UserPrincipal;
import com.example.trekking_app.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final AuthService authService;

    @PutMapping("/password/reset")
    public ResponseEntity<ApiResponse<PasswordResetResponse>> handlePasswordReset(@Valid @RequestBody PasswordResetRequest passwordResetRequest,
                                                                                  @AuthenticationPrincipal UserPrincipal user)

    {
        String requestId = UUID.randomUUID().toString();
        ApiResponse<PasswordResetResponse> response = authService.passwordReset(passwordResetRequest,user.getId());
        return ResponseEntity.status(HttpStatus.OK).headers(buildRequestHeaders()).body(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDetails>> handleGetUserProfile(@AuthenticationPrincipal UserPrincipal userPrincipal)
    {
        ApiResponse<UserDetails> response = authService.getUserInfo(userPrincipal.getId());
        return ResponseEntity.status(200).headers(buildRequestHeaders()).body(response);
    }

    private HttpHeaders buildRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-Id", UUID.randomUUID().toString());
        return headers;
    }
}
