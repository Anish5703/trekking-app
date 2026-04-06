package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.auth.SignupRequest;
import com.example.trekking_app.dto.auth.SignupResponse;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.entity.User;

public class UserMapper {

    public User toEntity(SignupRequest request)
    {
      User user = new User();
      user.setName(request.getName());
      user.setEmail(request.getEmail());
      user.setPassword(request.getPassword());
      user.setContact(request.getContact());
      user.setRole(request.getRole());
      user.setEmailVerified(false);
      return user;
    }

    public SignupResponse toSignupResponse(User user)
    {
        SignupResponse response = new SignupResponse();
        response.setName(user.getName());
        response.setContact(user.getContact());
        response.setEmail(user.getEmail());
        return response;
    }

}
