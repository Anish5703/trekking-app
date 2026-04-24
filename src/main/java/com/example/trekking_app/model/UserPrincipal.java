package com.example.trekking_app.model;

import com.example.trekking_app.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user)
    {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_"+user.getRole().name()));
    }

    @Override
    public String getUsername()
    {
        return user.getEmail();
    }

    @Override
    public String getPassword()
    {
        return user.getPassword();
    }

    @Override
    public boolean isEnabled(){return user.isEmailVerified();}

    public int getId(){return user.getId();}


}
