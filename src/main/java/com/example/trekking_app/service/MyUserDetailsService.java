package com.example.trekking_app.service;


import com.example.trekking_app.entity.User;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.model.UserPrincipal;
import com.example.trekking_app.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;

    public MyUserDetailsService(UserRepository userRepo)
    {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(@NonNull String email)
    {
        User user = userRepo.findByEmail(email)
                .orElseThrow(
                        () -> new ResourceNotFoundException("user","email",email)
                );
        return new UserPrincipal(user);

    }
}
