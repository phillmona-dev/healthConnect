package com.medco.HealthConnectProvider.services.impl.user;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.config.securityConfig.jwtTokenService.JwtService;
import com.medco.HealthConnectProvider.dto.PayerAdminDto;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.token.RefreshToken;
import com.medco.HealthConnectProvider.entity.user.Role;
import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.UnauthorizedException;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.user.RoleRepository;
import com.medco.HealthConnectProvider.repository.user.UserRepository;
import com.medco.HealthConnectProvider.services.mail.EmailService;
import com.medco.HealthConnectProvider.services.token.TokenService;
import com.medco.HealthConnectProvider.services.user.UserService;
import com.medco.HealthConnectProvider.ui.request.auth.password.ChangePasswordRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.LoginRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.token.RefreshTokenRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.user.SignUpRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.auth.JwtResponse;
import com.medco.HealthConnectProvider.ui.response.auth.RefreshTokenResponse;
import com.medco.HealthConnectProvider.ui.response.user.UserResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.mapper.MapperClass;
import com.medco.HealthConnectProvider.utils.paginationUtils.Pagination;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService JwtServiceImpl;
    private final PasswordEncoder passwordEncoder;

    private final TokenService tokenService;

    private RoleRepository roleRepository;
    private final PayerRepository payerRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    private EmailService emailService;


    public UserServiceImpl(UserRepository userRepository, AuthenticationManager authenticationManager, JwtService jwtServiceImpl, PasswordEncoder passwordEncoder, TokenService tokenService, RoleRepository roleRepository, PayerRepository payerRepository) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        JwtServiceImpl = jwtServiceImpl;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.roleRepository = roleRepository;
        this.payerRepository = payerRepository;
    }

    @Override
    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            if(authentication.isAuthenticated()) {

                RefreshToken refreshTokenEntity = tokenService.createRefreshToken(loginRequest.getEmail());

                String refreshToken = refreshTokenEntity.getToken();
                SecurityContextHolder.getContext().setAuthentication(authentication);

                UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();

                List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority())
                        .collect(Collectors.toList());

                String jwt = JwtServiceImpl.generateToken(userDetails.getEmail());


                return ResponseEntity.ok(new JwtResponse(jwt,refreshToken ,userDetails.getUserUuid(), userDetails.getEmail(),
                        userDetails.getFirstName(),userDetails.getFatherName(),userDetails.getGrandFatherName(),
                        userDetails.getMobilePhone(),userDetails.getPayerUuid(),userDetails.getProviderUuid(),userDetails.getAuthorities()));
            }else{
                throw new BadRequestException("Your Token is Expired try to login Again");
            }

        }catch (UnauthorizedException | BadRequestException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Username or password Provided! " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @Override
    public UserResponse createUser(SignUpRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new BadRequestException("Email is already in use!");
        }

        if (userRepository.existsByMobilePhone(signUpRequest.getMobilePhone())) {
            throw new BadRequestException("Mobile Phone is already in use!");
        }

        Role role = roleRepository.findByRoleUuid(signUpRequest.getRoleUuid());
        if (role == null){
            throw new BadRequestException("Can't Assign Role To User");
        }

        var user = new User();
        BeanUtils.copyProperties(signUpRequest, user);
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setRole(role);
        userRepository.save(user);

        var userResponse = new UserResponse();
        BeanUtils.copyProperties(user, userResponse);

        return userResponse;
    }

    @Override
    public UserResponse updateUser(String userUuid, SignUpRequest userRequest) {

        return userRepository.findByUserUuid(userUuid)
                .map(user -> {
                    var response = new UserResponse();
                    BeanUtils.copyProperties(userRequest,user);
                    BeanUtils.copyProperties(user,response);
                    return response;
                }).orElseThrow(() -> new BadRequestException("User With the Provided Id not found"));
    }

    @Override
    public UserResponse getUser(String userUuid) {
        return userRepository.findByUserUuid(userUuid)
                .map(MapperClass::mapToUserResponse).orElse(null);
    }

    @Override
    public ResponseEntity<?> deleteUser(String userUuid) {
        User user = userRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new BadRequestException("User With the provided Id not found"));
        user.setDeleted(true);

        userRepository.save(user);

        return ResponseEntity.ok("User Deleted Successfully");
    }

    @Override
    public PagedResponse<UserResponse> getAllSystemUsers(String search, String roleUuid, String providerUuid, int page, int limit) {
        Pageable pageable = Pagination.paginateResource(page, limit, "id", "desc");
        return (search != null)
                ? getAllUsersWithSearch(search, roleUuid, providerUuid, pageable)
                : getAllUsers(roleUuid, providerUuid, pageable);
    }

    @Override
    public ResponseEntity<?> changePassword(ChangePasswordRequest resetPasswordDetail, String userUuid) {

        var user = userRepository.findByUserUuid(userUuid).orElseThrow(() -> new BadRequestException("User Not Found"));

        if(!passwordEncoder.matches(user.getPassword(), resetPasswordDetail.getOldPassword())) throw new BadRequestException("Your Password is not correct!");

        if(!resetPasswordDetail.getNewPassword().equals(resetPasswordDetail.getConfirmPassword())) throw new BadRequestException("password did not match");

        user.setPassword(passwordEncoder.encode(resetPasswordDetail.getNewPassword()));

        userRepository.save(user);

        return ResponseEntity.ok("Password Changed Successfully");
    }

    @Override
    public RefreshTokenResponse getNewToken(RefreshTokenRequest tokenRequest) {
        return tokenService.findByToken(tokenRequest.getToken())
                .map(tokenService::verifyExpiration)
                .map(RefreshToken::getUserInfo)
                .map(userInfo -> {
                    String jwt = JwtServiceImpl.generateToken(userInfo.getEmail());

                    return RefreshTokenResponse.builder()
                            .newToken(jwt)
                            .refreshToken(tokenRequest.getToken())
                            .build();
                }).orElseThrow(() -> new BadRequestException("Couldn't generate new Token. try to login again...!"));
    }


    private PagedResponse<UserResponse> getAllUsers(String roleUuid, String providerUuid, Pageable pageable) {
        log.debug("Fetching all users with roleUuid={}, providerUuid={}, pageable={}", roleUuid, providerUuid, pageable);

        Page<User> userPage = userRepository.findAllByIsDeleted(false, pageable);

        log.info("Total elements found: {}", userPage.getTotalElements());
        log.info("Total pages: {}", userPage.getTotalPages());

        List<UserResponse> content = userPage.getContent().stream()
                .peek(user -> log.debug("Processing user: {}", user.getUserUuid()))
                .filter(user -> filterByRole(user, roleUuid))
                .filter(user -> filterByProvider(user, providerUuid))
                .map(this::mapToUserResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("Filtered content size: {}", content.size());

        return new PagedResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast()
        );
    }


    private PagedResponse<UserResponse> getAllUsersWithSearch(String search, String roleUuid, String providerUuid, Pageable pageable) {
        log.debug("Searching users with search={}, roleUuid={}, providerUuid={}, pageable={}", search, roleUuid, providerUuid, pageable);

        Page<User> userPage = userRepository.findAllByIsDeletedAndFirstNameContainingOrMobilePhoneContaining(false, search, search, pageable);

        log.info("Found {} users before filtering", userPage.getTotalElements());

        List<UserResponse> content = userPage.getContent().stream()
                .filter(user -> filterByRole(user, roleUuid))
                .filter(user -> filterByProvider(user, providerUuid))
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());

        log.info("Filtered to {} users", content.size());

        return new PagedResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast()
        );
    }

    private boolean filterByRole(User user, String roleUuid) {
        if (roleUuid == null) return true;
        if (user.getRole() == null) return false;
        boolean matches = roleUuid.equals(user.getRole().getRoleUuid().toString());
        log.debug("User {} role filter: {}", user.getUserUuid(), matches);
        return matches;
    }

    private boolean filterByProvider(User user, String providerUuid) {
        if (providerUuid == null) return true;
        boolean matches = providerUuid.equals(user.getProviderUuid());
        log.debug("User {} provider filter: {}", user.getUserUuid(), matches);
        return matches;
    }

    private UserResponse mapToUserResponse(User user) {
        try {
            UserResponse response = new UserResponse();
            BeanUtils.copyProperties(user, response);

            if(user.getRole() != null){
                response.setRoleName(user.getRole().getRoleName());
            }

            if (user.getUserStatus() != null){
                response.setUserStatus(user.getUserStatus());
            }
            return response;
        } catch (Exception e) {
            log.error("Error mapping user {} to UserResponse: {}", user.getUserUuid(), e.getMessage());
            return null;
        }
    }


    @Override
    public UserResponse createUser(PayerAdminDto payerAdminDto) {
        if (userRepository.existsByEmail(payerAdminDto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Error: Email is already in use!");
        }

        User user = new User();

        user.setEmail(payerAdminDto.getEmail());
        user.setGender(payerAdminDto.getGender());
        user.setTitle(payerAdminDto.getTitle());
        user.setFirstName(payerAdminDto.getFirstName());
        user.setFatherName(payerAdminDto.getFatherName());
        user.setGrandFatherName(payerAdminDto.getGrandFatherName());
        user.setMobilePhone(payerAdminDto.getMobilePhone());
        user.setPayerUuid(payerAdminDto.getPayerUuid());

        // Store the plain password temporarily for the email
        String plainPassword = payerAdminDto.getPassword();
        user.setPassword(passwordEncoder.encode(plainPassword));

        Role role = roleRepository.findByRoleUuid(payerAdminDto.getRoleUuid());
        if (role == null) {
            throw new BadRequestException("Role", "roleUuid", payerAdminDto.getRoleUuid());
        }
        user.setRole(role);

        if (payerAdminDto.getUserStatus() != null)
            user.setUserStatus(payerAdminDto.getUserStatus());
        else
            user.setUserStatus(Status.ACTIVE);

        User savedUser = userRepository.save(user);

        // Get payer name for the email
        String payerName = "your institution";
        if (user.getPayerUuid() != null) {
            Payer payer = payerRepository.findByPayerUuid(user.getPayerUuid());
            if (payer != null) {
                payerName = payer.getPayerName();
            }
        }

        String loginUrl = frontendUrl + "/login?newUser=true&email=" + user.getEmail();

        emailService.sendWelcomeEmail(
                user.getEmail(),
                user.getFirstName(),
                plainPassword,
                payerName,
                loginUrl
        );

        UserResponse userResponse = new UserResponse();
        BeanUtils.copyProperties(savedUser, userResponse);
        return userResponse;

    }
}
