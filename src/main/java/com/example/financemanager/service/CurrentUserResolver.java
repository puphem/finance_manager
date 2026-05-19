package com.example.financemanager.service;

import com.example.financemanager.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final UserDetailsServiceImpl userDetailsService;

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userDetailsService.loadUserEntityByUsername(username);
    }
}
