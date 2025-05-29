package com.medco.HealthConnectProvider.services.user;

import com.medco.HealthConnectProvider.ui.request.auth.password.ChangePasswordRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.LoginRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.token.RefreshTokenRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.user.SignUpRequest;
import com.medco.HealthConnectProvider.ui.response.auth.RefreshTokenResponse;
import com.medco.HealthConnectProvider.ui.response.user.UserResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface UserService {
    ResponseEntity<?> authenticateUser(LoginRequest loginRequest);

    UserResponse createUser(SignUpRequest signUpRequest);

    UserResponse updateUser(String userUuid, SignUpRequest userRequest);

    UserResponse getUser(String userUuid);

    ResponseEntity<?> deleteUser(String userUuid);

    List<UserResponse> getAllSystemUsers(String search,String roleUuid,String providerUuid, int page, int limit);

    ResponseEntity<?> changePassword(ChangePasswordRequest resetPasswordDetail, String userUuid);

    RefreshTokenResponse getNewToken(RefreshTokenRequest tokenRequest);
}
