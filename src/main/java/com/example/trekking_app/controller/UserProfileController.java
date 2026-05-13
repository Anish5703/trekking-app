package com.example.trekking_app.controller;

import com.example.trekking_app.dto.auth.PasswordResetRequest;
import com.example.trekking_app.dto.auth.PasswordResetResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.model.UserPrincipal;
import com.example.trekking_app.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return ResponseEntity.status(HttpStatus.OK).headers(buildSecureHeaders(requestId)).body(response);
    }

    private HttpHeaders buildSecureHeaders(String requestId) {
        HttpHeaders headers = new HttpHeaders();

        // Content negotiation
        headers.set("Content-Type", "application/json");

        // Prevent caching of auth responses
        headers.set("Cache-Control", "no-store, no-cache, must-revalidate, private");
        headers.set("Pragma", "no-cache");
        headers.set("Expires", "0");

        // Clickjacking protection
        headers.set("X-Frame-Options", "DENY");

        // Prevent MIME sniffing
        headers.set("X-Content-Type-Options", "nosniff");

        // XSS protection (legacy browsers)
        headers.set("X-XSS-Protection", "1; mode=block");

        // HSTS — enforce HTTPS for 1 year
        headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

        // Referrer control
        headers.set("Referrer-Policy", "no-referrer");

        // Distributed tracing / observability
        headers.set("X-Request-Id", requestId);

        return headers;
    }
}
