package com.example.trekking_app.service.user;

import com.example.trekking_app.entity.User;
import com.example.trekking_app.exception.resource.ResourceNotFoundException;
import com.example.trekking_app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MyUserDetailsServiceTest {
    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private MyUserDetailsService userDetailsService;

    @Test
    void userLoadedByUsernameSuccessfully() {
        User user = new User();
        user.setEmail("test@email.com");
        user.setPassword("password");
        when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        UserDetails response = userDetailsService.loadUserByUsername(user.getEmail());
        assertNotNull(response);
        assertEquals(user.getEmail(),response.getUsername());
        assertEquals("password",response.getPassword());
    }

    @Test
    void userNotFoundThrowsException() {
       String email = "test@email.com";
       when(userRepo.findByEmail(email)).thenReturn(Optional.empty());
       assertThrows(ResourceNotFoundException.class,()->userDetailsService.loadUserByUsername(email));
    }


}
