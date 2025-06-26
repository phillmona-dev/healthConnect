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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @PutMapping("/{userUuid}")
    //@PreAuthorize("hasRole('Update_User')")
    @Operation(summary = "Update System User", description = "Updates an existing user's information", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> updateUser(@PathVariable String userUuid, @Valid @RequestBody SignUpRequest userRequest) {
        UserResponse updatedUser = userService.updateUser(userUuid, userRequest);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping(path = "/{userUuid}")
    //@PreAuthorize("hasRole('Read_User')")
    @Operation(summary = "Read System User", description = "Retrieves a specific user's details by UUID", security = @SecurityRequirement(name = "bearerAuth"))
    public UserResponse getUser(@PathVariable String userUuid) {
        return userService.getUser(userUuid);

    }

    @GetMapping("/all")
    @Operation(
            summary = "Get all users",
            description = "Retrieves a paginated list of all users in the system. " +
                    "The list can be filtered using a search term and paginated. " +
                    "Page numbering starts from 1. " +
                    "The search parameter filters users based on the following fields:\n" +
                    "- First Name\n" +
                    "- Father Name\n" +
                    "- Mobile Phone\n" +
                    "- Email\n" +
                    "- Role UUID\n" +
                    "- Provider UUID\n" +
                    "- Payer UUID\n" +
                    "The search is case-insensitive and matches partial strings for text fields. " +
                    "For UUID fields, it requires an exact match.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful retrieval of user list",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PagedResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid page or limit parameters"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized access"),
                    @ApiResponse(responseCode = "403", description = "Forbidden access"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<PagedResponse<UserResponse>> getAllUsers(
            @Parameter(description = "Search term to filter users. Can match first name, father name, mobile phone, email, role UUID, provider UUID, or payer UUID.")
            @RequestParam(required = false) String search,

            @Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Fetching users with search={}, page={}, limit={}", search, page, limit);

        PagedResponse<UserResponse> response = userService.getAllSystemUsers(search, page - 1, limit);

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