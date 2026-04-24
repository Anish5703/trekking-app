package com.example.trekking_app.controller;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.oauth.OauthLoginResponse;
import com.example.trekking_app.service.OauthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oauth")
public class OauthController {

    private final OauthService oauthService;

    public OauthController(OauthService oauthService)
    {
        this.oauthService = oauthService;
    }


    @GetMapping("/login")
    public ResponseEntity<ApiResponse<OauthLoginResponse>> handleOauthLogin(Authentication authentication)
    {
      ApiResponse<OauthLoginResponse> response = oauthService.getOauthLogin(authentication);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type","application/json");
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);

    }

}
