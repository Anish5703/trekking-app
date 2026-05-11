package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.oauth.OauthLoginRequest;
import com.example.trekking_app.dto.oauth.OauthLoginResponse;
import com.example.trekking_app.service.OauthService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/*
@Tag(
        name = "OAuth2 Authentication",
        description = """
        This section covers Google and Facebook social login.
        
        As a frontend developer, you do not call most of these URLs directly
        with fetch or axios. The login flow works through browser redirects.
        Here is the full picture of how it works from start to finish.
        
        
        HOW THE LOGIN FLOW WORKS
        
        1. The user clicks "Login with Google" or "Login with Facebook" on your UI.
        
        2. You redirect the browser (not a fetch call) to one of these URLs:
        
             Google   -> http://localhost:8081/oauth2/authorization/google
             Facebook -> http://localhost:8081/oauth2/authorization/facebook
        
           Use window.location.href to do this, not fetch or axios.
           These URLs are handled by the backend internally.
        
        3. The user is taken to Google or Facebook to log in and approve access.
        
        4. After the user approves, the backend handles everything automatically
           (token exchange, user lookup or creation) and then redirects the browser to:
        
             Success -> http://localhost:8081/api/v1/oauth/login
             Failure -> Throws an LoginFailedException
        
        5. The success URL returns a JSON response (OauthLoginResponse) with the user details and JWT token.
           Read this response and store the jwtToken. Use it in the Authorization
           header for every subsequent API call:
        
             Authorization: Bearer <jwtToken>
        
        
        WHAT YOU GET BACK
        
        On success ApiResponse`<OauthLoginResponse>` dto is returned  ,
        On Failure ApiResponse`<ErrorResponse>` dto is returned
        
        """
)

 */

@RestController
@RequestMapping("/api/v1/oauth")
public class OauthController {

    private final OauthService oauthService;

    public OauthController(OauthService oauthService)
    {
        this.oauthService = oauthService;
    }


    @Hidden
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<OauthLoginResponse>> handleOauthLogin(Authentication authentication)
    {
      ApiResponse<OauthLoginResponse> response = oauthService.getOauthLogin(authentication);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type","application/json");
        return ResponseEntity.status(200).headers(headers).body(response);

    }

    @PostMapping("/app/login")
    public ResponseEntity<ApiResponse<OauthLoginResponse>> handleAppOauthLogin(@NonNull @RequestBody OauthLoginRequest oauthLoginRequest)
    {
      ApiResponse<OauthLoginResponse> response = oauthService.getAppOauthLogin(oauthLoginRequest);
      return ResponseEntity.status(200).body(response);
    }


}
