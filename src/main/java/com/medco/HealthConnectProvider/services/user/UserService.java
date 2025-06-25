package com.medco.HealthConnectProvider.services.user;

import com.medco.HealthConnectProvider.dto.PayerAdminDto;
import com.medco.HealthConnectProvider.ui.request.auth.password.ChangePasswordRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.LoginRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.token.RefreshTokenRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.user.SignUpRequest;
import com.medco.HealthConnectProvider.ui.response.auth.RefreshTokenResponse;
import com.medco.HealthConnectProvider.ui.response.user.UserResponse;
import org.springframework.http.ResponseEntity;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;

import java.util.List;

public interface UserService {
    ResponseEntity<?> authenticateUser(LoginRequest loginRequest);

    UserResponse createUser(SignUpRequest signUpRequest);

    UserResponse updateUser(String userUuid, SignUpRequest userRequest);

    UserResponse getUser(String userUuid);

    ResponseEntity<?> deleteUser(String userUuid);


    ResponseEntity<?> changePassword(ChangePasswordRequest resetPasswordDetail, String userUuid);

    RefreshTokenResponse getNewToken(RefreshTokenRequest tokenRequest);

    UserResponse createUser(PayerAdminDto payerAdminDto);

    PagedResponse<UserResponse> getAllSystemUsers(String search, int page, int limit);

}
