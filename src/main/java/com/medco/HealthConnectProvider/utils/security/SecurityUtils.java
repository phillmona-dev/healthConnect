package com.medco.HealthConnectProvider.utils.security;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserDetailsImpl;
import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class SecurityUtils {

    private static UserDetailsService userDetailsService;

    @Autowired
    public SecurityUtils(UserDetailsService userDetailsService) {
        SecurityUtils.userDetailsService = userDetailsService;
    }

    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);

    public static UserDetailsImpl getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error: Login to get authorized.");
        }

        return (UserDetailsImpl) authentication.getPrincipal();
    }

    public String getAuthenticatedUserProviderUuid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new SecurityException("No authentication found in SecurityContext");
        }

        Object principal = authentication.getPrincipal();
        UserPrincipal userPrincipal;

        if (principal instanceof UserPrincipal) {
            userPrincipal = (UserPrincipal) principal;
        } else if (principal instanceof String) {
            // Fetch user details using UserDetailsService
            userPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername((String) principal);
        } else {
            throw new SecurityException("Unexpected principal type: " + principal.getClass().getName());
        }

        String providerUuid = userPrincipal.getProviderUuid();
        if (providerUuid == null || providerUuid.isEmpty()) {
            throw new SecurityException("Authenticated user does not have an associated provider UUID");
        }

        return providerUuid;
    }

}
