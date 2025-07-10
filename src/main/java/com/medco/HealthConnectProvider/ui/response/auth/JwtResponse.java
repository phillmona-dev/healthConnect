package com.medco.HealthConnectProvider.ui.response.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
    private String token;
    private String refreshToken;
    private String userUuid;
    private String email;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String mobilePhone;
    private String payerUuid;
    private String providerUuid;
    private String roleUuid;
    private String roleName;
    private Set<String> authorities;
    private String profilePicture;
    private String Logo;
    private String companyName;
    private boolean firstTimeLogin;
    private byte[] imageData;
}
