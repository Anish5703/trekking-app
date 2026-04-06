package com.example.trekking_app.service;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.user.UserDetails;
import com.example.trekking_app.entity.Token;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.exception.auth.UserNotFoundException;
import com.example.trekking_app.exception.user.DeleteUserFailedException;
import com.example.trekking_app.mapper.UserMapper;
import com.example.trekking_app.repository.TokenRepository;
import com.example.trekking_app.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AdminService {

    private final UserRepository userRepo;
    private final TokenRepository tokenRepo;
    private final UserMapper userMapper;

    public AdminService(UserRepository userRepo, TokenRepository tokenRepo)
    {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.userMapper = new UserMapper();
    }
   @Transactional
    public ApiResponse<List<UserDetails>> getUserList()
    {
        List<User> users = userRepo.findAll();
        List<UserDetails> userDetailsList = new ArrayList<>();
        users.stream()
                .forEach(
                        user -> userDetailsList.add(userMapper.toUserDetails(user))
                );
        String message = "List of users retrieved";
        return new ApiResponse<>(userDetailsList,message,200);
    }
    @Transactional
    public ApiResponse<UserDetails> deleteUser(int id)
    {
        try {
            User user = userRepo.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            Optional<Token> token = tokenRepo.findByUserId(user.getId());
            if (token.isPresent())
                tokenRepo.delete(token.get());
            userRepo.delete(user);
            UserDetails userDetails = userMapper.toUserDetails(user);
            String message = "User removed";
            return new ApiResponse<>(userDetails,message,200);
        }
        catch (Exception e)
        {
            throw new DeleteUserFailedException("Failed to delete user");
        }
    }
}
