package com.medco.HealthConnectProvider.utils.auth;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtil {

    public static UserPrincipal getCurrentUser(){
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();

            return userDetails;
        }catch (Exception ex){
            throw new RuntimeException(ex.getMessage());
        }
    }
}
