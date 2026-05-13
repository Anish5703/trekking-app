package com.example.trekking_app.mapper;

import com.example.trekking_app.entity.Token;
import com.example.trekking_app.entity.User;

import java.time.LocalDateTime;

public class TokenMapper {


    public Token toEntity(String tokenName , User user , LocalDateTime expiryAt)
    {
        return Token.builder()
                .tokenName(tokenName)
                .user(user)
                .expiryAt(expiryAt)
                .build();
    }
}
