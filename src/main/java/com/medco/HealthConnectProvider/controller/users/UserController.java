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
import com.medco.HealthConnectProvider.ui.response.user.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider/healthConnectProvider/users")
public class UserController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final PasswordService passwordService;

    public UserController(AuthenticationManager authenticationManager, UserService userService, PasswordService passwordService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.passwordService = passwordService;
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return userService.authenticateUser(loginRequest);
    }

    @PostMapping("/signup")
    //@PreAuthorize("hasRole('Create_User')")
    //@Operation(summary = "Add System User", security = @SecurityRequirement(name = "bearerAuth"))
    public UserResponse createUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        return userService.createUser(signUpRequest);
    }

    @PutMapping(path = "/{userUuid}")
    @PreAuthorize("hasRole('Update_User')")
    @Operation(summary = "Update System User", security = @SecurityRequirement(name = "bearerAuth"))
    public UserResponse updateUser(@PathVariable String userUuid, @RequestBody SignUpRequest userRequest) {
        return userService.updateUser(userUuid, userRequest);

    }

    @GetMapping(path = "/{userUuid}")
    @PreAuthorize("hasRole('Read_User')")
    @Operation(summary = "Read System User", security = @SecurityRequirement(name = "bearerAuth"))
    public UserResponse getUser(@PathVariable String userUuid) {
        return userService.getUser(userUuid);

    }

    @GetMapping("/all")
    public List<UserResponse> getAllSystemUsers(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "filterByRole", required = false) String roleUuid,
            @RequestParam(value = "filterByProvider", required = false) String providerUuid,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit
    ){
        return userService.getAllSystemUsers(search,roleUuid,providerUuid,page,limit);
    }

    @DeleteMapping(path = "/{userUuid}")
    @PreAuthorize("hasRole('Delete_User')")
    @Operation(summary = "Delete System User", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> deleteUser(@PathVariable String userUuid) {
        return userService.deleteUser(userUuid);
    }

//    @GetMapping("")
    ////    @PreAuthorize("hasRole('Read-Payer-Users')")
//    @Operation(summary = "Read Payer Users", security = @SecurityRequirement(name = "bearerAuth"))
//    public List<UserResponse> getPayerUsers(@RequestParam(value="page", defaultValue = "1") int page,
//                                            @RequestParam(value="limit", defaultValue = "25") int limit) {
//        return userService.getPayerUsers(page,limit);
//
//    }
//
//
//
//    @GetMapping(path = "/all")
//    @PreAuthorize("hasRole('Read-Users')")
//    @Operation(summary = "Read All System Users", security = @SecurityRequirement(name = "bearerAuth"))
//    public List<UserResponse> getUsers(@RequestParam(value="page", defaultValue = "1") int page,
//                                       @RequestParam(value="limit", defaultValue = "25") int limit){
//        return userService.getUsers(page,limit);
//
//    }
//
//    @PostMapping(path = "/search")
//    @PreAuthorize("hasRole('Read-Users')")
//    @Operation(summary = "Search All System Users", security = @SecurityRequirement(name = "bearerAuth"))
//    public List<UserResponse> searchUsers(@RequestParam("search") String searchKey, @RequestParam(value="page", defaultValue = "1") int page,
//                                          @RequestParam(value="limit", defaultValue = "25") int limit){
//        return userService.searchUsers(searchKey,page,limit);
//
//    }
//
//
//    @PostMapping(path = "/uploadprofile")
//    @PreAuthorize("hasRole('Change-User-Profile')")
//    @Operation(summary = "Change-User-Profile", security = @SecurityRequirement(name = "bearerAuth"))
//    public ResponseEntity<?> uploadProfilePicture(@ModelAttribute UploadProfileRequest requestDetail)
//            throws IOException {
//        return userService.uploadProfilePicture(requestDetail);
//    }

    @PutMapping(path = "/changepassword/{userUuid}")
    // @PreAuthorize("hasRole('Change-Password')")
    @Operation(summary = "Change-Password", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest resetPasswordDetail,
                                            @PathVariable String userUuid) {
        return userService.changePassword(resetPasswordDetail, userUuid);

    }
    //
//
//    @GetMapping(path = "/email/verification/{emailVerificationToken}")
//    @PreAuthorize("hasRole('Email-Verification')")
//    @Operation(summary = "Email-Verification", security = @SecurityRequirement(name = "bearerAuth"))
//    public ResponseEntity<?> verifyAccount(@PathVariable String emailVerificationToken) {
//        return userService.verifyAccount(emailVerificationToken);
//
//    }
//
//    @PutMapping(path = "/email/verification/resend/{email}")
//    @PreAuthorize("hasRole('Email-Verification')")
//    @Operation(summary = "Send Email Verification Code", security = @SecurityRequirement(name = "bearerAuth"))
//    public ResponseEntity<?> reSendVerification(@PathVariable String email)
//            throws AddressException, MessagingException, IOException {
//        return userService.reSendVerification(email);
//
//    }
//
//    @PutMapping(path = "/password/sendresetcode/{email}")
//    public ResponseEntity<?> resetPassword(@PathVariable String email)
//            throws AddressException, MessagingException, IOException {
//        return userService.sendPasswordResetCode(email);
//
//    }
//
    @PutMapping(path = "/password/resetPassword")
    public ResponseEntity<?> checkResetCode(@RequestBody ResetPasswordRequest resetPassword) {
        return passwordService.resetPassword(resetPassword);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request){
        return passwordService.forgotPassword(request);
    }

    @PostMapping("/refresh-token")
    public RefreshTokenResponse refreshTokenEndpoint(@Valid @RequestBody RefreshTokenRequest tokenRequest){
        return userService.getNewToken(tokenRequest);
    }
}
