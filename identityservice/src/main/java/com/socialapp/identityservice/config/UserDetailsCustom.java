package com.socialapp.identityservice.config;

import com.socialapp.identityservice.entity.Identity;
import com.socialapp.identityservice.service.IdentityService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component("userDetailsService")
public class UserDetailsCustom implements UserDetailsService {
    private final IdentityService identityService;

    public UserDetailsCustom(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Identity identity = this.identityService.findIdentityByEmail(email);
        if (identity == null) {
            throw new UsernameNotFoundException("username/password khong hop le");
        }
        return new User(
                identity.getEmail(),
                identity.getPassword(),
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
    }

}