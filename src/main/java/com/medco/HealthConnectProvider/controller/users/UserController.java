package com.medco.HealthConnectProvider.controller.users;

import com.medco.HealthConnectProvider.services.password.PasswordService;
import com.medco.HealthConnectProvider.services.user.UserService;
import com.medco.HealthConnectProvider.ui.request.auth.password.ChangePasswordRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.ForgotPasswordRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.LoginRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.ResetPasswordRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.token.RefreshTokenRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.user.SignUpRequest;
import com.medco.HealthConnectProvider.ui.response.auth.RefreshTokenResponse;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.user.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/healthConnect/users")
@Tag(name = "User Management", description = "APIs for managing system users, authentication, and user operations")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final PasswordService passwordService;

    public UserController(AuthenticationManager authenticationManager, UserService userService, PasswordService passwordService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.passwordService = passwordService;
    }

    @PostMapping("/signin")
    @Operation(summary = "User login", description = "Authenticates a user and returns a JWT token")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return userService.authenticateUser(loginRequest);
    }

    @PostMapping("/signup")
    //@PreAuthorize("hasRole('Create_User')")
    @Operation(summary = "Add System User", description = "Creates a new user in the system", security = @SecurityRequirement(name = "bearerAuth"))
    public UserResponse createUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        return userService.createUser(signUpRequest);
    }

    @PutMapping(path = "/{userUuid}")
    @PreAuthorize("hasRole('Update_User')")
    @Operation(summary = "Update System User", description = "Updates an existing user's information", security = @SecurityRequirement(name = "bearerAuth"))
    public UserResponse updateUser(@PathVariable String userUuid, @RequestBody SignUpRequest userRequest) {
        return userService.updateUser(userUuid, userRequest);

    }

    @GetMapping(path = "/{userUuid}")
    @PreAuthorize("hasRole('Read_User')")
    @Operation(summary = "Read System User", description = "Retrieves a specific user's details by UUID", security = @SecurityRequirement(name = "bearerAuth"))
    public UserResponse getUser(@PathVariable String userUuid) {
        return userService.getUser(userUuid);

    }

    @GetMapping("/all")
    public ResponseEntity<PagedResponse<UserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String roleUuid,
            @RequestParam(required = false) String providerUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Fetching users with search={}, roleUuid={}, providerUuid={}, page={}, limit={}",
                search, roleUuid, providerUuid, page, limit);

        PagedResponse<UserResponse> response = userService.getAllSystemUsers(search, roleUuid, providerUuid, page, limit);

        log.info("Returned {} users", response.getContent().size());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping(path = "/{userUuid}")
    //@PreAuthorize("hasRole('Delete_User')")
    @Operation(summary = "Delete System User", description = "Deletes a user from the system by UUID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> deleteUser(@PathVariable String userUuid) {
        return userService.deleteUser(userUuid);
    }

    @PutMapping(path = "/changepassword/{userUuid}")
    // @PreAuthorize("hasRole('Change-Password')")
    @Operation(summary = "Change Password", description = "Allows a user to change their password", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest resetPasswordDetail,
                                            @PathVariable String userUuid) {
        return userService.changePassword(resetPasswordDetail, userUuid);

    }

    @PutMapping(path = "/password/resetPassword")
    @Operation(summary = "Reset Password", description = "Resets a user's password using a verification code")
    public ResponseEntity<?> checkResetCode(@RequestBody ResetPasswordRequest resetPassword) {
        return passwordService.resetPassword(resetPassword);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot Password", description = "Initiates the password recovery process by sending a reset code")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request){
        return passwordService.forgotPassword(request);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh Token", description = "Generates a new JWT token using a valid refresh token")
    public RefreshTokenResponse refreshTokenEndpoint(@Valid @RequestBody RefreshTokenRequest tokenRequest){
        return userService.getNewToken(tokenRequest);
    }
}