package com.example.trekking_app.service;

import com.example.trekking_app.dto.auth.SignupRequest;
import com.example.trekking_app.dto.auth.SignupResponse;
import com.example.trekking_app.dto.global.ApiMessage;
import com.example.trekking_app.entity.Admin;
import com.example.trekking_app.entity.Customer;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.exception.auth.DuplicateEmailFoundException;
import com.example.trekking_app.exception.auth.EmptySignupFieldException;
import com.example.trekking_app.exception.auth.SignupFailedException;
import com.example.trekking_app.mapper.UserMapper;
import com.example.trekking_app.model.Role;
import com.example.trekking_app.repository.AdminRepository;
import com.example.trekking_app.repository.CustomerRepository;
import com.example.trekking_app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthService {
    private final UserRepository userRepo;
    private final AdminRepository adminRepo;
    private final CustomerRepository customerRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepo ,AdminRepository adminRepo
            ,CustomerRepository customerRepo,UserMapper userMapper
            ,BCryptPasswordEncoder passwordEncoder)
    {
        this.userRepo = userRepo;
        this.adminRepo = adminRepo;
        this.customerRepo = customerRepo;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public SignupResponse signupUser(SignupRequest request)
    { try {
        this.validateSignupRequest(request);
        User newUser = null;
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole().equals(Role.ADMIN)) {
            Admin admin = (Admin) user;
            newUser = adminRepo.save(admin);
        } else if (user.getRole().equals(Role.CUSTOMER)) {
            Customer customer = (Customer) user;
            newUser = customerRepo.save(customer);
        }
        if (newUser == null)
            throw new SignupFailedException("Failed to save new user");
        else {
            /*
            *Logic to send confirmation mail to user
             */
            ApiMessage message = new ApiMessage(200, "New user created");
            return userMapper.toSignupResponse(newUser, message);
        }
    }
    catch(Exception e)
    {
        log.error("Database Exception : {}",e.getLocalizedMessage());
        throw new SignupFailedException("Failed to save new user");
    }
    }

    public void validateSignupRequest(SignupRequest request)
    {
        try {
            if (request.getName().isEmpty() || request.getEmail().isEmpty() || request.getPassword().isEmpty())
                throw new EmptySignupFieldException("Signup fields cannot be empty");

            if (userRepo.existsByEmail(request.getEmail())) {
                log.error("User with email {} already exists", request.getEmail());
                throw new DuplicateEmailFoundException("User with email already exists");
            }
        }
        catch(Exception e)
        {
            throw new SignupFailedException("Failed to save new user");
        }
    }
}
