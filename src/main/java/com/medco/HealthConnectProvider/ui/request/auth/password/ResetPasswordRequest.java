package com.medco.HealthConnectProvider.ui.request.auth.password;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    private String passwordResetCode;
    private String newPassword;
    private String confirmPassword;
}
