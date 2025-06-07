package com.medco.HealthConnectProvider.ui.response.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

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
    private Collection<? extends GrantedAuthority> authorities;


}
