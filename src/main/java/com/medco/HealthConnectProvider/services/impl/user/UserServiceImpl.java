package com.medco.HealthConnectProvider.services.impl.user;

import com.medco.HealthConnectProvider.config.LogoGeter.LogoGetter;
import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.config.securityConfig.jwtTokenService.JwtService;
import com.medco.HealthConnectProvider.dto.PayerAdminDto;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.entity.token.RefreshToken;
import com.medco.HealthConnectProvider.entity.user.Role;
import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.exception.UnauthorizedException;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.repository.user.RoleRepository;
import com.medco.HealthConnectProvider.repository.user.UserRepository;
import com.medco.HealthConnectProvider.services.mail.EmailService;
import com.medco.HealthConnectProvider.services.payer.PayerService;
import com.medco.HealthConnectProvider.services.providers.ProviderService;
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
import com.medco.HealthConnectProvider.utils.Image.ImageUtils;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.mapper.MapperClass;
import com.medco.HealthConnectProvider.utils.paginationUtils.Pagination;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService JwtServiceImpl;
    private final PasswordEncoder passwordEncoder;

    private final TokenService tokenService;

    private final RoleRepository roleRepository;
    private final PayerRepository payerRepository;
//    private final PayerService payerService;
    private final ProviderService providerService;

    @Autowired
    private LogoGetter logoGetter;


    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ProviderRepository providerRepository;

    public UserServiceImpl(UserRepository userRepository, AuthenticationManager authenticationManager, JwtService jwtServiceImpl, PasswordEncoder passwordEncoder, TokenService tokenService, RoleRepository roleRepository, PayerRepository payerRepository, ProviderService providerService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        JwtServiceImpl = jwtServiceImpl;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.roleRepository = roleRepository;
        this.payerRepository = payerRepository;

        this.providerService = providerService;

    }


    @Override
    public ResponseEntity<?> authenticateUser(LoginRequest loginRequest) {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            if(authentication.isAuthenticated()) {
                RefreshToken refreshTokenEntity = tokenService.createRefreshToken(loginRequest.getEmail());
                String refreshToken = refreshTokenEntity.getToken();
                SecurityContextHolder.getContext().setAuthentication(authentication);

                UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();

                User user = userRepository.findByEmail(loginRequest.getEmail())
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + loginRequest.getEmail()));

                boolean firstTime=user.isFirstTimeLogin();
                if (!user.isFirstTimeLogin()){
                    user.setFirstTimeLogin(true);
                    userRepository.save(user);
                }
                logger.info("User found: {}", user.getEmail());

                String jwt = JwtServiceImpl.generateToken(userDetails.getEmail());

                // Directly access payerUuid and providerUuid from the User object
                String payerUuid = user.getPayerUuid();
                String providerUuid = user.getProviderUuid();

                logger.info("PayerUuid from user: {}", payerUuid);
                logger.info("ProviderUuid from user: {}", providerUuid);

                // Convert authorities to set of privilege names
                Set<String> authorities = userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());

                String profilePicture=user.getProfilePicture();
                String logo = "";
                String companyName = "";
                if (user.getProviderUuid()!=null){
                    ResponseEntity<ByteArrayResource>  providerLogoResponse=providerService.getProviderLogo(user.getProviderUuid());
                    Provider provider=providerRepository.findByProviderUuid(userDetails.getProviderUuid());
                    companyName=provider.getProviderName();
                    logo=convertTo64Bit(providerLogoResponse);


                } else if (user.getPayerUuid()!=null) {
                    ResponseEntity<ByteArrayResource>  payerLogoResponse= logoGetter.PayerLogo(user.getPayerUuid());
                    Payer payer=payerRepository.findByPayerUuid(user.getPayerUuid());
                    companyName=payer.getPayerName();
                    logo= convertTo64Bit(payerLogoResponse);

                }

                if (user.getPayer()!=null) {
                    companyName = user.getPayer().getPayerName();
                    System.out.println("company name  payer " + companyName);
                }

                if (user.getProvider()!=null) {

                    companyName = user.getProvider().getProviderName();
                    log.info("user's company : {}",companyName);
                    System.out.println("company name1 "+companyName);
                }
                System.out.println("company name2 "+companyName);
                logger.info("Authorities: {}", authorities);
                 byte[] imageData = new byte[0];
                if (user.getImageData()!=null)
                    imageData=ImageUtils.decompressImage(user.getImageData());


                JwtResponse response = new JwtResponse(
                        jwt,
                        refreshToken,
                        user.getUserUuid(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getFatherName(),
                        user.getGrandFatherName(),
                        user.getMobilePhone(),
                        payerUuid,
                        providerUuid,
                        authorities,
                        profilePicture,
                        logo,
                        companyName,
                        firstTime,
                        imageData
               

                );

                logger.info("JwtResponse created: payerUuid={}, providerUuid={}", response.getPayerUuid(), response.getProviderUuid());

                return ResponseEntity.ok(response);
            } else {
                throw new BadRequestException("Your Token is Expired try to login Again");
            }
        } catch (UnauthorizedException | BadRequestException e) {
            logger.error("Authentication failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Username or password Provided! " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during authentication", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    private String convertTo64Bit(ResponseEntity<ByteArrayResource> logoResponse) {
        String base64Logo = "";
        if (logoResponse != null && logoResponse.getBody() != null) {
            byte[] logoBytes = logoResponse.getBody().getByteArray();
             base64Logo = Base64.getEncoder().encodeToString(logoBytes);
           


        }
        return base64Logo;
    }


    @Override
    @Transactional
    public UserResponse createUser(SignUpRequest signUpRequest) {


        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        log.info("Starting user creation process for email: {}", signUpRequest.getEmail());

        // Validate email and mobile phone
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            log.warn("Email already in use: {}", signUpRequest.getEmail());
            throw new BadRequestException("Email is already in use!");
        }
        if (userRepository.existsByMobilePhone(signUpRequest.getMobilePhone())) {
            log.warn("Mobile phone already in use: {}", signUpRequest.getMobilePhone());
            throw new BadRequestException("Mobile Phone is already in use!");
        }

        // Find the role
        Role role = roleRepository.findByRoleUuid(signUpRequest.getRoleUuid());
        if (role == null) {
            log.error("Role not found for UUID: {}", signUpRequest.getRoleUuid());
            throw new BadRequestException("Can't Assign Role To User");
        }
        log.info("Role found: {}", role.getRoleName());

        // Create and populate user object
        User user = new User();
        BeanUtils.copyProperties(signUpRequest, user);
        log.info("User object created and populated");

        // Generate a random password
        String randomPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(randomPassword));
        user.setRole(role);

        // Set user status if not provided
        if (user.getUserStatus() == null) {
            user.setUserStatus(Status.ACTIVE);
        }

        log.info("User status set to: {}", user.getUserStatus());

        // Determine the institution type and set Payer or Provider
        String institutionName = "your institution";
//        if (role.getRoleName().startsWith("PA_")) {
//            log.info("Processing payer role");
//            Payer payer = payerRepository.findByPayerUuid(role.getPayerUuid());
//            if (payer == null) {
//                log.error("No payer found for UUID: {}", role.getPayerUuid());
//                throw new BadRequestException("No payer found for the given role");
//            }
//            user.setPayerUuid(payer.getPayerUuid());
//            institutionName = payer.getPayerName();
//            log.info("User associated with payer: {}", payer.getPayerName());
//        } else if (role.getRoleName().startsWith("PR_")) {
//            log.info("Processing provider role");
//            Provider provider = providerRepository.findByProviderUuid(role.getProviderUuid());
//            if (provider == null) {
//                log.error("No provider found for UUID: {}", role.getProviderUuid());
//                throw new BadRequestException("No provider found for the given role");
//            }
//            user.setProviderUuid(provider.getProviderUuid());
//            institutionName = provider.getProviderName();
//            log.info("User associated with provider: {}", provider.getProviderName());
//        } else {
//            log.warn("Role is neither payer nor provider: {}", role.getRoleName());
//
//
//        }

        if (userDetails.getProviderUuid()!=null){
            Provider provider=providerRepository.findByProviderUuid(userDetails.getProviderUuid());
            if (provider==null)throw new BadRequestException("provider not found");
            user.setProvider(provider);
            user.setProviderUuid(userDetails.getProviderUuid());
        } else if (userDetails.getPayerUuid()!=null) {
            Payer payer=payerRepository.findByPayerUuid(userDetails.getPayerUuid());
            if (payer==null)throw new BadRequestException("payer not found");
            user.setPayer(payer);
            user.setPayerUuid(userDetails.getPayerUuid());
        }


        // Save the user
        User savedUser = userRepository.save(user);
        log.info("User saved to database with ID: {}", savedUser.getId());

        // Log the saved user details
        log.info("Saved user details - PayerUuid: {}, ProviderUuid: {}", savedUser.getPayerUuid(), savedUser.getProviderUuid());

        // Send welcome email
        String loginUrl = frontendUrl + "/login?newUser=true&email=" + savedUser.getEmail();
        try {
            emailService.sendWelcomeEmail(
                    savedUser.getEmail(),
                    savedUser.getFirstName(),
                    randomPassword,
                    institutionName,
                    loginUrl
            );
            log.info("Welcome email sent to: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to user {}: {}", savedUser.getEmail(), e.getMessage());
        }

        // Prepare and return the response
        UserResponse userResponse = new UserResponse();
        BeanUtils.copyProperties(savedUser, userResponse);
        userResponse.setPayerUuid(savedUser.getPayerUuid());
        userResponse.setProviderUuid(savedUser.getProviderUuid());
        userResponse.setRoleName(role.getRoleName());

        log.info("User creation process completed for email: {}", savedUser.getEmail());
        return userResponse;
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 12; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    private String determineInstitutionName(Role role) {
        // This method should determine the institution name based on the role
        // You might need to adjust this based on your role naming convention
        if (role.getRoleName().endsWith("_Manager")) {
            return role.getRoleName().replace("_Manager", "");
        }
        // Default case if we can't determine the institution name
        return "HealthConnect";
    }

    @Override
    @Transactional
    public UserResponse updateUser(String userUuid, SignUpRequest userRequest) {
        return userRepository.findByUserUuid(userUuid)
                .map(user -> {

                    user.setEmail(userRequest.getEmail());
                    user.setTitle(userRequest.getTitle());
                    user.setFirstName(userRequest.getFirstName());
                    user.setFatherName(userRequest.getFatherName());
                    user.setGrandFatherName(userRequest.getGrandFatherName());
                    user.setGender(userRequest.getGender());
                    user.setMobilePhone(userRequest.getMobilePhone());
                    user.setUserStatus(userRequest.getUserStatus());

                    if (StringUtils.hasText(userRequest.getRoleUuid())) {
                        Role role = roleRepository.findByRoleUuid(userRequest.getRoleUuid());
                        if (role == null){
                            throw new BadRequestException("Invalid role UUID");
                        }
                        user.setRole(role);
                    }

                    User updatedUser = userRepository.save(user);
                    return mapUserToUserResponse(updatedUser);
                })
                .orElseThrow(() -> new ResourceNotFoundException("User", "UUID", userUuid));
    }

    private UserResponse mapUserToUserResponse(User user) {
        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(user, response);
        response.setRoleName(user.getRole() != null ? user.getRole().getRoleName() : null);
        return response;
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
    public PagedResponse<UserResponse> getAllSystemUsers(String search, int page, int limit) {
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        log.debug("Searching users with search={}, page={}, limit={}", search, page, limit);

        Pageable pageable = Pagination.paginateResource(page, limit, "id", "desc");
        Page<User> userPage = userRepository.findAll(UserSpecification.searchUsers(search), pageable);

        log.info("Found {} users", userPage.getTotalElements());

        List<UserResponse> content = userPage.getContent().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());

        log.info("Mapped {} users to response", content.size());

        return new PagedResponse<>(
                content,
                userPage.getNumber() + 1,
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast()
        );
    }

    @Transactional
    @Override
    public ResponseEntity<?> changeProfile(MultipartFile profilePicture) throws IOException {
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        User user=userRepository.findByUserUuid(userDetails.getUserUuid()).orElseThrow(()-> new BadRequestException("user not found"));
        byte[] profilePictureBytes = profilePicture.getBytes();

        if (profilePictureBytes.length > 1048576)
            throw new BadRequestException("Profile should be less than 1mb");

        user.setImageData(ImageUtils.compressImage(profilePictureBytes));
        userRepository.save(user);

        return ResponseEntity.ok("Your profile picture changed successfully");

    }


    @Override
    public ResponseEntity<?> changePassword(ChangePasswordRequest resetPasswordDetail, String userUuid) {

        var user = userRepository.findByUserUuid(userUuid).orElseThrow(() -> new BadRequestException("User Not Found"));

        if(!passwordEncoder.matches( resetPasswordDetail.getOldPassword(),user.getPassword())) throw new BadRequestException("Your Password is not correct!");

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
            if (user.getImageData()!=null)
                 response.setImageData( ImageUtils.decompressImage(user.getImageData()));

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
