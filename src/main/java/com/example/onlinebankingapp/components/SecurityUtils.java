package com.example.onlinebankingapp.components;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.onlinebankingapp.entities.CustomerEntity;

public class SecurityUtils {
    // Method to retrieve the currently logged-in user
    public CustomerEntity getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null &&
                authentication.getPrincipal() instanceof CustomerEntity selectedUser) {
            // Check if the user is active
            if(!selectedUser.isActive()) {
                return null;
            }
            return (CustomerEntity) authentication.getPrincipal();
        }
        return null;
    }
}
