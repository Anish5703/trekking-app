package com.example.trekking_app.dto.auth;

import com.example.trekking_app.dto.global.ApiMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SignupResponse {

    private String name;
    private String email;
    private String contact;
    private ApiMessage message;
}
