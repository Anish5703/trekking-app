package com.example.trekking_app.controller;

import com.example.trekking_app.dto.auth.LoginRequest;
import com.example.trekking_app.dto.auth.LoginResponse;
import com.example.trekking_app.dto.auth.SignupRequest;
import com.example.trekking_app.dto.auth.SignupResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(
        name = "Authentication",
        description = "Endpoints for user signup, email verification, login, password reset , forgot password"
)
@RestController
@RequestMapping("/api/v1/auth/")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Register a new user",
            description = """
            Creates a new user account and sends a confirmation email with a verification link.
            The user must verify their email before they can log in.
            """,
            tags = {"Authentication"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully. Confirmation email sent.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                    {
                      "status": 201,
                      "message": "Check mail for confirmation link",
                      "data": {
                        "id": 33,
                        "name": "Anish Paudel",
                        "email": "garunddigital@gmail.com",
                        "contact": "+977 9857077777",
                        "role" : "CUSTOMER"
                   
                      }
                    }
                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body (e.g. missing fields, malformed email)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "status": 400,
                      "message": "Failed to save new user",
                      "data": {
                        "errorType" : "SIGNUP_FAILED" ,
                      "details": "Must be a valid field" }
                    }
                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Email already registered",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "status": 409,
                      "message": "Email already registered",
                      "data": {
                      "errorType" : "DUPLICATE_EMAIL_FOUND",
                      "details": "An account with this email already exists"  }
                    }
                    """)
                    )
            ),
    })

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> handleUserSignup(
            @Valid @RequestBody SignupRequest signupRequest,
            HttpServletRequest servletRequest)
    {
        String requestId = UUID.randomUUID().toString();
        ApiResponse<SignupResponse> response = authService.signupUser(signupRequest, servletRequest);
        return ResponseEntity.status(HttpStatus.CREATED).headers(buildSecureHeaders(requestId)).body(response);
    }

    @Operation(
            summary = "Confirm email address",
            description = """
            Validates the one-time email confirmation token sent during signup.
            On success, marks the user's email as verified and allows login.
            """,
            tags = {"Authentication"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Email verified successfully. User may now log in.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "status": 200,
                      "message": "Email verified. You can now log in",
                      "data": {
                       "name": "Anish Paudel",
                      "email": "garunddigital@gmail.com",
                      "password": "SecureP@ssw0rd!",
                      "contact": "+977 985707577777",
                      "role": "CUSTOMER"
                      }
                    }
                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid token (malformed , missing ,etc",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "status": 400,
                      "message": "Failed to validate signup token",
                      "data": {
                        "errorType" : "SIGNUP_FAILED" ,
                      "details": "Failed to validate signup token" }
                    }
                    """)
                    )
            )
    })
    @GetMapping("/signup/confirmation")
    public ResponseEntity<ApiResponse<SignupResponse>> handleSignupConfirmation(
            @Parameter(
                    name = "token",
                    description = "One-time email verification token sent to the user's inbox",
                    required = true,
                    example = "3f2a1b4c-verification-token-example"
            )
            @RequestParam(name = "token") String token)
    {
        String requestId = UUID.randomUUID().toString();
        ApiResponse<SignupResponse> response = authService.validateSignupConfirmationToken(token);
        return ResponseEntity.status(HttpStatus.OK).headers(buildSecureHeaders(requestId)).body(response);
    }

    @Operation(
            summary = "Resend email confirmation link",
            description = """
            Generates a new email verification token and resends the confirmation email.
            The previous token is invalidated.
            """,
            tags = {"Authentication"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Confirmation email resent successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "status": 200,
                      "message": "Check mail for confirmation link",
                      "data": {
                       "name": "Anish Paudel",
                      "email": "garunddigital@gmail.com",
                      "contact": "+977 985707577777",
                      "role": "CUSTOMER"
                      }
                    }
                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid or missing email",
            content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(value = """
                    {
                      "status": 400,
                      "message": "Failed to validate signup token",
                      "data": {
                        "errorType" : "SIGNUP_FAILED" ,
                      "details": "Invalid signup token" }
                    }
                    """)
    )
            )
            ,
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "No account found with this email",
            content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(value = """
                    {
                      "status": 404,
                      "message": "Email not registered fill the signup form",
                      "data": {
                        "errorType" : "USER_NOT_FOUND" ,
                      "details": "No user account found" }
                    }
                    """)
    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Email is already verified",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "status": 409,
                      "message": "Email is already verified.You can go to login page",
                      "data": {
                        "errorType" : "SIGNUP_FAILED" ,
                      "details": "Email is already verified" }
                    }
                    """)
                    )
            ),
    })
    @PutMapping("/signup/resend")
    public ResponseEntity<ApiResponse<SignupResponse>> handleResendSignupConfirmation(
            @Parameter(
                    name = "email",
                    description = "The registered email address to resend the confirmation link to",
                    required = true,
                    example = "garunddigital@gmail.com"
            )
            @RequestParam(name="email") @Email @NotBlank String email,
            HttpServletRequest servletRequest)
    {
        String requestId = UUID.randomUUID().toString();
        ApiResponse<SignupResponse> response = authService.resendSignupConfirmation(email, servletRequest);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type","application/json");
        return ResponseEntity.status(HttpStatus.OK).headers(buildSecureHeaders(requestId)).body(response);
    }

    @Operation(
            summary = "Login user",
            description = """
            Authenticates a user with their email and password.
            Returns a short-lived JWT access token on success.
            Access token expires in 5 hours.
            """,
            tags = {"Authentication"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful. JWT tokens returned",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                    {
                      "status": 200,
                      "message": "Login successful.",
                      "data": {
                        "name": "Anish Paudel",
                      "email": "garunddigital@gmail.com",
                      "contact": "+977 985707577777",
                      "role": "CUSTOMER",
                       "jwtToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      }
                    }
                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid email or password",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "status": "error",
                      "message": "Credentials didn't matched",
                      "data":
                      "errorType" : "LOGIN_FAILED",
                      "details": "Failed to log in user"
                    }
                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Email not verified",
            content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(value = """
                    {
                      "status": 403,
                      "message": "Email not verified. Check email inbox for confirmation or apply resend confirmation",
                      "data": {
                        "errorType" : "NOT_VERIFIED" ,
                      "details": "Email not verified. Verify first" }
                    }
                    """)
    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Email not found",
            content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(value = """
                    {
                      "status": 404,
                      "message": "Email not registered fill the signup form",
                      "data": {
                        "errorType" : "USER_NOT_FOUND" ,
                      "details": "No user account found" }
                    }
                    """)
    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> handleUserLogin(@Valid @RequestBody LoginRequest loginRequest)
    {
        String requestId = UUID.randomUUID().toString();
        ApiResponse<LoginResponse> response = authService.loginUser(loginRequest);

        return ResponseEntity.status(HttpStatus.OK).headers(buildSecureHeaders(requestId)).body(response);
    }


    /**
     * Builds a consistent set of security-hardened HTTP response headers
     * for all auth endpoints.
     * Note: X-Frame-Options, HSTS, CSP etc. are ideally handled globally
     * via Spring Security config or an OncePerRequestFilter — kept here
     * for controllers that bypass the security filter chain.
     */
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
