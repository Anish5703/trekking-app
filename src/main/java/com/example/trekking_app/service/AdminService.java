package com.example.trekking_app.service;

import com.example.trekking_app.dto.admin.AccountStatusResetRequest;
import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.user.UserDetails;
import com.example.trekking_app.entity.Token;
import com.example.trekking_app.entity.User;
import com.example.trekking_app.exception.auth.EmailNotVerifiedException;
import com.example.trekking_app.exception.auth.UserNotFoundException;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.exception.resource.ResourceUpdateFailedException;
import com.example.trekking_app.exception.user.DeleteUserFailedException;
import com.example.trekking_app.mapper.UserMapper;
import com.example.trekking_app.model.Role;
import com.example.trekking_app.repository.TokenRepository;
import com.example.trekking_app.repository.UserRepository;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public ApiResponse<Page<UserDetails>> getUsers(@NonNull Role role ,Integer page , Integer size)
    {
        Pageable pageable = PageRequest.of(page , size);
        Page<UserDetails> users = userRepo.findByRoleOrderByTimeStampDesc(role,pageable).map(userMapper::toUserDetails);
        String message = "List of users retrieved";
        return new ApiResponse<>(users,message,200);
    }
    @Transactional
    public ApiResponse<UserDetails> deleteUser(Integer id)
    {
        try {
            User user = userRepo.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            Optional<Token> token = tokenRepo.findByUser_Id(user.getId());
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

    @Transactional(readOnly = true)
    public ApiResponse<Page<UserDetails>> getInactiveUsers(@NonNull Role role , Integer page ,Integer size)
    {
        Pageable pageable = PageRequest.of(page,size);
      Page<UserDetails> inactiveUsers = userRepo.findByRoleAndIsActiveFalseOrderByTimeStampDesc(role,pageable).map(userMapper::toUserDetails);
      String message = inactiveUsers.isEmpty() ? String.format("no inactive %s found",role) : String.format("deactivated %s fetched",role);
      return new ApiResponse<>(inactiveUsers,message,200);

    }
    @Transactional(readOnly = true)
    public ApiResponse<Page<UserDetails>> getActiveUsers(@NonNull Role role,Integer page,Integer size)
    {
        Pageable pageable = PageRequest.of(page,size);
        Page<UserDetails> activeUsers = userRepo.findByRoleAndIsActiveTrueOrderByTimeStampDesc(role,pageable).map(userMapper::toUserDetails);
        String message = activeUsers.isEmpty() ? String.format("no active %s found",role) : String.format("active %s fetched",role);
        return new ApiResponse<>(activeUsers,message,200);
    }


    @Transactional
    public ApiResponse<Void> updateAccountStatus(AccountStatusResetRequest accountStatusResetRequest)
    {
        User user = userRepo.findById(accountStatusResetRequest.getUserId()).orElseThrow(
                () -> new ResourceNotFoundException("user","id",accountStatusResetRequest.getUserId())
        );
        //hardcoded super admin email
        if(user.getEmail().equals("admin@admin.com")) throw new ResourceUpdateFailedException("this is a super admin account not allowed for status update");

        if(!user.isEmailVerified()) throw new EmailNotVerifiedException("email must be verified first");
        user.setActive(accountStatusResetRequest.isActive());
        User savedUser = userRepo.save(user);

        String message="";
        if(savedUser.getRole().equals(Role.ADMIN))  message = savedUser.isActive() ? "admin account is activated" : "admin account is deactivated";
        else if(savedUser.getRole().equals(Role.CUSTOMER)) message = savedUser.isActive() ? "customer account is activated" : "customer account is deactivated";
        else throw new UserNotFoundException("invalid user role");

        return new ApiResponse<>(null,message,200);
    }
}
