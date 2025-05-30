package com.medco.HealthConnectProvider.utils.security;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserDetailsImpl;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

public class SecurityUtils {

    public static UserDetailsImpl getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error: Login to get authorized.");
        }

        return (UserDetailsImpl) authentication.getPrincipal();
    }
}
