package com.example.trekking_app.mapper;

import com.example.trekking_app.dto.auth.LoginResponse;
import com.example.trekking_app.dto.auth.SignupRequest;
import com.example.trekking_app.dto.auth.SignupResponse;
import com.example.trekking_app.dto.user.UserDetails;
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
        response.setId(user.getId());
        response.setName(user.getName());
        response.setContact(user.getContact());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        return response;
    }

    public LoginResponse toLoginResponse(User user)
    {
        LoginResponse response = new LoginResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setContact(user.getContact());
        response.setRole(user.getRole());
        return response;
    }

    public UserDetails toUserDetails(User user)
    {
        UserDetails response = new UserDetails();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setContact(user.getContact());
        response.setRole(user.getRole());
        return response;
    }

}
