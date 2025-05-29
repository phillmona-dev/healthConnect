package com.medco.HealthConnectProvider.ui.request.auth.password;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    private String email;
    private String password;
}
