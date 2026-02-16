package com.platform.user.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper to extract current user info from SecurityContext.
 * Avoids scattering SecurityContextHolder calls throughout the codebase.
 */
@Component
public class SecurityContextProvider {

    /**
     * Get the currently authenticated user's email.
     */
    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return auth.getName(); // We set email as the principal
    }

    /**
     * Get the currently authenticated user's ID.
     * Stored as credentials in UsernamePasswordAuthenticationToken.
     */
    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            return null;
        }
        return (Long) auth.getCredentials();
    }

    /**
     * Check if the current user has a specific role.
     */
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
