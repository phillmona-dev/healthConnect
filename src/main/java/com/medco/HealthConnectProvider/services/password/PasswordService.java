package com.medco.HealthConnectProvider.services.password;

import com.medco.HealthConnectProvider.ui.request.auth.password.ForgotPasswordRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.ResetPasswordRequest;
import org.springframework.http.ResponseEntity;

public interface PasswordService {
    ResponseEntity<?> forgotPassword(ForgotPasswordRequest request);

    ResponseEntity<?> resetPassword(ResetPasswordRequest resetPassword);
}
