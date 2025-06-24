package com.medco.HealthConnectProvider.services.impl.payer;


import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.dto.PayerAdminDto;
import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.entity.user.Role;
import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.claims.ClaimRepository;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.user.RoleRepository;
import com.medco.HealthConnectProvider.repository.user.UserRepository;
import com.medco.HealthConnectProvider.services.mail.EmailService;
import com.medco.HealthConnectProvider.services.payer.PayerService;
import com.medco.HealthConnectProvider.services.user.UserService;
import com.medco.HealthConnectProvider.ui.request.auth.password.payer.PayerRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.providers.ProviderRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.user.SignUpRequest;
import com.medco.HealthConnectProvider.ui.request.claims.ClaimReviewRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.claims.ClaimResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PayerProviderResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PayerResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PolicyHolderListResponse;
import com.medco.HealthConnectProvider.ui.response.provider.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import com.medco.HealthConnectProvider.utils.SortUtils;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

import java.util.Base64;
import org.springframework.data.jpa.domain.Specification;
import com.medco.HealthConnectProvider.specifications.PayerSpecifications;

@Service
@Slf4j
public class PayerServiceImpl implements PayerService {

    private final Logger logger = LoggerFactory.getLogger(PayerService.class);

    private final PayerRepository payerRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;
    private final ContractRepository contractRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Value("${file.upload-dir-payer-logos:C:/Users/Administrator/OneDrive/Desktop/MedcoProjects/logos/payers}")
    private String payerLogosDirectory;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public PayerServiceImpl(PayerRepository payerRepository, RoleRepository roleRepository, UserService userService, ContractRepository contractRepository) {
        this.payerRepository = payerRepository;
        this.roleRepository = roleRepository;
        this.userService = userService;
        this.contractRepository = contractRepository;
    }


    @Transactional
    @Override
    public PayerResponse createPayer(@Valid PayerRequest payerRequest, MultipartFile logo) {

        if (payerRepository.existsByEmail(payerRequest.getEmail())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Error: Duplicated Payer Email is not allowed.");
        }

        if (payerRepository.existsByTelephone(payerRequest.getTelephone())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Error: Duplicated Payer Telephone is not allowed.");
        }

        if (payerRepository.existsByPayerName(payerRequest.getPayerName())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Error: Duplicated Payer Name is not allowed.");
        }

        Payer payer = new Payer();
        BeanUtils.copyProperties(payerRequest, payer);
        payer.setPayerUuid(UUID.randomUUID().toString());
        payer.setRegistrationDate(new Date());
        payer.setStatus(payerRequest.getStatus() != null ? payerRequest.getStatus() : Status.PENDING);

        if (logo != null && !logo.isEmpty()) {
            try {
                File directory = new File(payerLogosDirectory);
                if (!directory.exists()) {
                    directory.mkdirs();
                    logger.info("Created directory: {}", payerLogosDirectory);
                }

                String fileName = logo.getOriginalFilename();
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                String newFileName = "logo_" + UUID.randomUUID().toString() + "." + extension;

                Path path = Paths.get(payerLogosDirectory + "/" + newFileName);
                Files.write(path, logo.getBytes());
                logger.info("Saved logo to: {}", path);

                payer.setLogoPath(newFileName);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error uploading logo: " + e.getMessage());
            }
        }

        Payer savedPayer = payerRepository.save(payer);
        log.info("Payer created with UUID: {}, status: {}", savedPayer.getPayerUuid(), savedPayer.getStatus());

        Role role = new Role();
        String payerNameForRole = payerRequest.getPayerName();
        if (payerNameForRole.length() > 40) {
            payerNameForRole = payerNameForRole.substring(0, 40);
        }
        role.setRoleName(payerNameForRole + "_Manager");
        role.setProviderUuid(savedPayer.getPayerUuid());
        role.setRoleDescription("Manages the system for " + payerRequest.getPayerName());
        Role savedRole = roleRepository.save(role);

        PayerResponse payerResponse = getPayerResponse(savedPayer);
        payerResponse.setRoleUuid(savedRole.getRoleUuid());

        if (savedPayer.getLogoPath() != null) {
            try {
                Path path = Paths.get(payerLogosDirectory + "/" + savedPayer.getLogoPath());
                byte[] fileContent = Files.readAllBytes(path);
                String base64Logo = Base64.getEncoder().encodeToString(fileContent);
                payerResponse.setLogoBase64("data:image/png;base64," + base64Logo);
            } catch (IOException e) {
                log.warn("Could not read logo for payer {}: {}", savedPayer.getPayerUuid(), e.getMessage());
                payerResponse.setLogoBase64("");
            }
        } else {
            payerResponse.setLogoBase64("");
        }

        return payerResponse;
    }

    private void createPayerManager(PayerRequest payerRequest, Role savedRole, Payer savedPayer) {

        SignUpRequest managerDto = new SignUpRequest();
        managerDto.setEmail(payerRequest.getEmail());
        managerDto.setGender("Male");
        managerDto.setTitle("Mr");
        managerDto.setFirstName("Payer");
        managerDto.setFatherName("Manager");
        managerDto.setGrandFatherName("Default");
        managerDto.setMobilePhone(payerRequest.getTelephone());

        String randomPassword = generateRandomPassword();
        managerDto.setPassword(randomPassword);

        managerDto.setRoleUuid(savedRole.getRoleUuid());
        //managerDto.setProviderUuid(savedProvider.getProviderUuid());
        managerDto.setUserStatus(Status.ACTIVE);

        // Create the user
        try {
            User savedUser = createPayerManagerUser(managerDto, savedPayer);

            // Send welcome email
            String loginUrl = frontendUrl + "/login?newUser=true&email=" + savedUser.getEmail();
            emailService.sendWelcomeEmail(
                    savedUser.getEmail(),
                    savedUser.getFirstName(),
                    randomPassword,
                    savedPayer.getPayerName(),
                    loginUrl
            );
        } catch (Exception e) {
            System.err.println("Failed to create provider manager: " + e.getMessage());
        }
    }

    private User createPayerManagerUser(SignUpRequest managerDto, Payer savedPayer) {
        if (userRepository.existsByEmail(managerDto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Error: Email is already in use!");
        }

        User user = new User();
        BeanUtils.copyProperties(managerDto, user);
        user.setPassword(passwordEncoder.encode(managerDto.getPassword()));

        Role role = roleRepository.findByRoleUuid(managerDto.getRoleUuid());
        if (role == null) {
            throw new BadRequestException("Role not found");
        }
        user.setRole(role);
        user.setPayerUuid(savedPayer.getPayerUuid());

        return userRepository.save(user);
    }

    private static PayerAdminDto getPayerAdminDto(PayerRequest payerRequest, Role savedRole, Payer payer1) {
        PayerAdminDto payerAdminDto = new PayerAdminDto();
        payerAdminDto.setEmail(payerRequest.getEmail());
        payerAdminDto.setGender("Male");
        payerAdminDto.setTitle("Mr");
        payerAdminDto.setFirstName("Institution" );
        payerAdminDto.setFatherName("Admin");
        payerAdminDto.setGrandFatherName("Default");
        payerAdminDto.setMobilePhone(payerRequest.getTelephone());

        String randomPassword = generateRandomPassword();
        payerAdminDto.setPassword(randomPassword);

        payerAdminDto.setRoleUuid(savedRole.getRoleUuid());
        payerAdminDto.setPayerUuid(payer1.getPayerUuid());
        return payerAdminDto;
    }

    private static String generateRandomPassword() {
        // Generate a random password with 6 characters
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        String password = sb.toString();
        System.out.println("Generated Password: " + password);
        return password;
    }

    @Override
    @Transactional
    public PayerResponse updatePayer(String payerUuid, PayerRequest payerRequest, MultipartFile logo) {
        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        List<Payer> payerName = payerRepository
                .findAllByPayerName(payerRequest.getPayerName());

        if (payer == null)
            throw new ResourceNotFoundException("Payer", "PayerUuid", payerUuid);

        // Check for duplicate name, but exclude current payer from the check
        if (!payer.getPayerName().equals(payerRequest.getPayerName()) &&
                payerName.stream().anyMatch(p -> !p.getPayerUuid().equals(payerUuid))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Error: Duplicated Payer Name is not allowed.");
        }

        BeanUtils.copyProperties(payerRequest, payer);
        if (payerRequest.getStatus() != null)
            payer.setStatus(payerRequest.getStatus());
        else
            payer.setStatus(Status.PENDING);

        // Update logo if provided
        if (logo != null && !logo.isEmpty()) {
            try {
                // Ensure directory exists
                File directory = new File(payerLogosDirectory);
                if (!directory.exists()) {
                    directory.mkdirs();
                    logger.info("Created directory: {}", payerLogosDirectory);
                }

                // Delete old logo if exists
                if (payer.getLogoPath() != null && !payer.getLogoPath().isEmpty()) {
                    Path oldLogoPath = Paths.get(payerLogosDirectory + "/" + payer.getLogoPath());
                    try {
                        Files.deleteIfExists(oldLogoPath);
                        logger.info("Deleted old logo: {}", oldLogoPath);
                    } catch (IOException e) {
                        // Log error but continue with update
                        logger.warn("Failed to delete old logo: {}", e.getMessage());
                    }
                }

                String fileName = logo.getOriginalFilename();
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                String newFileName = "logo_" + UUID.randomUUID().toString() + "." + extension;

                Path path = Paths.get(payerLogosDirectory + "/" + newFileName);
                Files.write(path, logo.getBytes());
                logger.info("Saved updated logo to: {}", path);

                payer.setLogoPath(newFileName);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error uploading logo: " + e.getMessage());
            }
        }

        payerRepository.save(payer);
        return getPayerResponse(payer);
    }


    @Override
    public ResponseEntity<?> updatePayerStatus(String payerUuid, Status payerStatus) {
        Payer payer=payerRepository.findByPayerUuid(payerUuid);
        payer.setStatus(payerStatus);
        payerRepository.save(payer);
        return ResponseEntity.ok("your institution is " + payerStatus);
    }

    @Override
    public PayerResponse getPayer(String payerUuid) {
        Payer payer = payerRepository.findByPayerUuid(payerUuid);

        if (payer == null)
            throw new ResourceNotFoundException("Payer", "payerUuid", payerUuid);

        PayerResponse response = getPayerResponse(payer);

        // Add logo as base64 if available
        if (payer.getLogoPath() != null && !payer.getLogoPath().isEmpty()) {
            try {
                String logoPath = payerLogosDirectory + "/" + payer.getLogoPath();
                File logoFile = new File(logoPath);

                if (logoFile.exists() && logoFile.isFile()) {
                    byte[] fileContent = Files.readAllBytes(logoFile.toPath());
                    String base64Logo = Base64.getEncoder().encodeToString(fileContent);
                    response.setLogoBase64("data:" + determineContentType(logoPath) + ";base64," + base64Logo);
                } else {
                    // Set default logo if payer logo doesn't exist
                    setDefaultLogoBase64(response);
                }
            } catch (IOException e) {
                logger.warn("Could not read logo for payer {}: {}", payer.getPayerUuid(), e.getMessage());
                // Set default logo on error
                setDefaultLogoBase64(response);
            }
        } else {
            // Set default logo if payer has no logo path
            setDefaultLogoBase64(response);
        }

        return response;
    }

    @Override
    public List<PayerResponse> getPayers(String search, int page, int limit, Status status) {
        if (page > 0)
            page = page - 1;
        Pageable pageRequest = PageRequest.of(page, limit, Sort.by("id").descending());
        Page<Payer>
                payer = search!=null?payerRepository.findAllByIsDeletedAndPayerNameContainingAndStatus(false, search,
                status, pageRequest): payerRepository.findAllByIsDeletedAndStatus(false, status, pageRequest);
        long totalPages = payer.getTotalPages();
        List<Payer> institutionList = payer.getContent();

        List<PayerResponse> institutionResponse = new ArrayList<>();
        for (Payer p : institutionList) {
            PayerResponse pr = new PayerResponse();
            if (institutionResponse.isEmpty())
                pr.setTotalPages(totalPages);
            BeanUtils.copyProperties(p, pr);

            // Get total contracts for this payer
            Long contractCount = contractRepository.countByPayerPayerUuidAndIsDeleted(p.getPayerUuid(), false);
            pr.setTotalContracts(contractCount);

            institutionResponse.add(pr);
        }
        return institutionResponse;
    }

    @Override
    public List<PolicyHolderListResponse> getPolicyHolders(String search, int page, int limit, Status status) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, limit, Sort.by("id").ascending());
        return contractRepository.findPolicyHoldersList(search, status, pageable);
    }

    @Override
    public List<PayerProviderResponse> getProviderPolicyHolders(String providerUuid, String payerUuid) {
        return contractRepository.findProviderPolicyHolders(providerUuid, payerUuid);
    }

    @Override
    public ResponseEntity<?> deletePayer(String payerUuid) {
        Payer institution = payerRepository.findByPayerUuid(payerUuid);
        if (institution == null)
            throw new ResourceNotFoundException("Institution", "institutionUuid", payerUuid);

        institution.setDeleted(true);
        payerRepository.save(institution);
        return ResponseEntity.ok(new MessageResponse("Institution deleted successfully!"));
    }

    @Override
    public ResponseEntity<ByteArrayResource> getPayerLogo(String payerUuid) {
        try {
            Payer payer = payerRepository.findByPayerUuid(payerUuid);
            if (payer == null || payer.getLogoPath() == null || payer.getLogoPath().isEmpty()) {
                logger.warn("Payer logo not found for UUID: {}", payerUuid);
                return serveDefaultLogo();
            }

            String logoPath = payerLogosDirectory + "/" + payer.getLogoPath();
            logger.debug("Attempting to load logo from path: {}", logoPath);

            File logoFile = new File(logoPath);
            if (!logoFile.exists() || !logoFile.isFile()) {
                logger.warn("Logo file does not exist at path: {}", logoPath);
                return serveDefaultLogo();
            }

            Path path = logoFile.toPath();
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(determineContentType(logoPath)))
                    .contentLength(logoFile.length())
                    .body(resource);
        } catch (IOException e) {
            logger.error("Error retrieving payer logo: {}", e.getMessage(), e);
            return serveDefaultLogo();
        }
    }

    private ResponseEntity<ByteArrayResource> serveDefaultLogo() {
        try {
            // Path to a default logo in your resources folder
            Resource resource = new ClassPathResource("static/images/default-payer-logo.png");
            if (resource.exists()) {
                ByteArrayResource byteResource = new ByteArrayResource(
                        FileCopyUtils.copyToByteArray(resource.getInputStream()));

                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .contentLength(resource.contentLength())
                        .body(byteResource);
            }

            // If default logo doesn't exist, return not found
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            logger.error("Error serving default logo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public PagedResponse<PayerResponse> getPayersWithFilters(String searchKey, int page, int limit, Status status, String category, String payerName, Long tinNumber, String level, String sortBy, String sortDir) {

        if (page > 0) {
            page = page - 1;
        }

        String validatedSortBy = SortUtils.validatePayerSortField(sortBy);
        String validatedSortDir = SortUtils.validateSortDirection(sortDir);

        Sort sort = validatedSortDir.equalsIgnoreCase("asc") ?
                Sort.by(validatedSortBy).ascending() :
                Sort.by(validatedSortBy).descending();

        Pageable pageRequest = PageRequest.of(page, limit, sort);

        Specification<Payer> spec = PayerSpecifications.isNotDeleted();

        if (searchKey != null && !searchKey.isEmpty()) {
            spec = spec.and(PayerSpecifications.containsSearchKey(searchKey));
        }

        if (status != null) {
            spec = spec.and(PayerSpecifications.hasStatus(status));
        }

        if (category != null && !category.isEmpty()) {
            spec = spec.and(PayerSpecifications.hasCategory(category));
        }

        if (payerName != null && !payerName.isEmpty()) {
            spec = spec.and(PayerSpecifications.hasPayerName(payerName));
        }

        if (tinNumber != null) {
            spec = spec.and(PayerSpecifications.hasTinNumber(tinNumber));
        }

        if (level != null && !level.isEmpty()) {
            spec = spec.and(PayerSpecifications.hasLevel(level));
        }

        Page<Payer> payerPage = payerRepository.findAll(spec, pageRequest);
        List<Payer> payerList = payerPage.getContent();

        List<PayerResponse> payerResponses = new ArrayList<>();
        for (Payer payer : payerList) {
            PayerResponse response = new PayerResponse();

            BeanUtils.copyProperties(payer, response);
            response.setStatus(payer.getStatus());

            Long contractCount = contractRepository.countByPayerPayerUuidAndIsDeleted(
                    payer.getPayerUuid(), false);
            response.setTotalContracts(contractCount);

            if (payer.getLogoPath() != null && !payer.getLogoPath().isEmpty()) {
                try {
                    String logoPath = payerLogosDirectory + "/" + payer.getLogoPath();
                    File logoFile = new File(logoPath);

                    if (logoFile.exists() && logoFile.isFile()) {
                        byte[] fileContent = Files.readAllBytes(logoFile.toPath());
                        String base64Logo = Base64.getEncoder().encodeToString(fileContent);
                        response.setLogoBase64("data:" + determineContentType(logoPath) + ";base64," + base64Logo);
                    } else {
                        setDefaultLogoBase64(response);
                    }
                } catch (IOException e) {
                    logger.warn("Could not read logo for payer {}: {}", payer.getPayerUuid(), e.getMessage());
                    setDefaultLogoBase64(response);
                }
            } else {
                setDefaultLogoBase64(response);
            }

            payerResponses.add(response);
        }

        PagedResponse<PayerResponse> pagedResponse = new PagedResponse<>();
        pagedResponse.setContent(payerResponses);
        pagedResponse.setPage(payerPage.getNumber() + 1);
        pagedResponse.setPerPage(payerPage.getSize());
        pagedResponse.setTotalElements(payerPage.getTotalElements());
        pagedResponse.setTotalPages(payerPage.getTotalPages());
        pagedResponse.setHasNext(payerPage.hasNext());
        pagedResponse.setHasPrevious(payerPage.hasPrevious());

        return pagedResponse;

    }

    private void setDefaultLogoBase64(PayerResponse response) {
        try {
            Resource resource = new ClassPathResource("static/images/default-payer-logo.png");
            if (resource.exists()) {
                byte[] fileContent = FileCopyUtils.copyToByteArray(resource.getInputStream());
                String base64Logo = Base64.getEncoder().encodeToString(fileContent);
                response.setLogoBase64("data:image/png;base64," + base64Logo);
            }
        } catch (IOException e) {
            logger.warn("Could not read default logo: {}", e.getMessage());
        }
    }

    private String determineContentType(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private String getContentType(String path) {
        String extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    private PayerResponse getPayerResponse(Payer payer) {
        PayerResponse response = new PayerResponse();
        BeanUtils.copyProperties(payer, response);
        response.setStatus(payer.getStatus());

        if (payer.getLogoPath() != null && !payer.getLogoPath().isEmpty()) {
            try {
                String logoPath = payerLogosDirectory + "/" + payer.getLogoPath();
                File logoFile = new File(logoPath);

                if (logoFile.exists() && logoFile.isFile()) {
                    byte[] fileContent = Files.readAllBytes(logoFile.toPath());
                    String base64Logo = Base64.getEncoder().encodeToString(fileContent);
                    response.setLogoBase64("data:" + determineContentType(logoPath) + ";base64," + base64Logo);
                } else {
                    setDefaultLogoBase64(response);
                }
            } catch (IOException e) {
                logger.warn("Could not read logo for payer {}: {}", payer.getPayerUuid(), e.getMessage());
                setDefaultLogoBase64(response);
            }
        } else {
            setDefaultLogoBase64(response);
        }

        return response;
    }


    @Override
    public Page<ClaimResponse> getClaimsForReview(int page, int size) {
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Claim> claims = claimRepository.findByPayerUuidAndStatus(payerUuid, ClaimStatus.UNDER_REVIEW, pageable);

        return claims.map(this::mapClaimToClaimResponse);
    }

    @Override
    @Transactional
    public ResponseEntity<?> reviewClaim(String claimUuid, ClaimReviewRequest reviewRequest) {
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        if (!claim.getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("You don't have access to review this claim");
        }

        if (!claim.getStatus().equals(ClaimStatus.UNDER_REVIEW)) {
            throw new BadRequestException("This claim is not available for review");
        }

        ClaimStatus newStatus = reviewRequest.isApproved() ? ClaimStatus.APPROVED : ClaimStatus.REJECTED;
        claim.setStatus(newStatus);
        claim.setReviewComment(reviewRequest.getComment());
        claim.setReviewedByUuid(userDetails.getUserUuid());
        claim.setReviewedAt(Instant.now());

        claimRepository.save(claim);

        // TODO: Implement notification to provider about claim review result

        return ResponseEntity.ok(new MessageResponse("Claim reviewed successfully"));
    }

    @Override
    public PagedResponse<PayerResponse> getPayersWithFiltersWithOutLogo(String searchKey, int page, int limit,
                                                             Status status, String category, String payerName, Long tinNumber, String level,
                                                             String sortBy, String sortDir) {

        if (page > 0) {
            page = page - 1;
        }

        String validatedSortBy = SortUtils.validatePayerSortField(sortBy);
        String validatedSortDir = SortUtils.validateSortDirection(sortDir);

        Sort sort = validatedSortDir.equalsIgnoreCase("asc") ?
                Sort.by(validatedSortBy).ascending() :
                Sort.by(validatedSortBy).descending();

        Pageable pageable = PageRequest.of(page, limit, sort);

        Specification<Payer> spec = PayerSpecifications.isNotDeleted();

        if (searchKey != null && !searchKey.isEmpty()) {
            spec = spec.and(PayerSpecifications.containsSearchKey(searchKey));
        }

        if (status != null) {
            spec = spec.and(PayerSpecifications.hasStatus(status));
        }

        if (category != null && !category.isEmpty()) {
            spec = spec.and(PayerSpecifications.hasCategory(category));
        }

        if (payerName != null && !payerName.isEmpty()) {
            spec = spec.and(PayerSpecifications.hasPayerName(payerName));
        }

        if (tinNumber != null) {
            spec = spec.and(PayerSpecifications.hasTinNumber(tinNumber));
        }

        if (level != null && !level.isEmpty()) {
            spec = spec.and(PayerSpecifications.hasLevel(level));
        }

        Page<Payer> payerPage = payerRepository.findAll(spec, pageable);
        List<Payer> payerList = payerPage.getContent();

        List<PayerResponse> payerResponses = new ArrayList<>();
        for (Payer payer : payerList) {
            PayerResponse response = new PayerResponse();
            BeanUtils.copyProperties(payer, response);
            response.setStatus(payer.getStatus());

            Long contractCount = contractRepository.countByPayerPayerUuidAndIsDeleted(
                    payer.getPayerUuid(), false);
            response.setTotalContracts(contractCount);

            payerResponses.add(response);
        }

        PagedResponse<PayerResponse> pagedResponse = new PagedResponse<>();
        pagedResponse.setContent(payerResponses);
        pagedResponse.setPage(payerPage.getNumber() + 1);
        pagedResponse.setPerPage(payerPage.getSize());
        pagedResponse.setTotalElements(payerPage.getTotalElements());
        pagedResponse.setTotalPages(payerPage.getTotalPages());
        pagedResponse.setHasNext(payerPage.hasNext());
        pagedResponse.setHasPrevious(payerPage.hasPrevious());

        return pagedResponse;
    }

    private ClaimResponse mapClaimToClaimResponse(Claim claim) {
        ClaimResponse response = new ClaimResponse();
        BeanUtils.copyProperties(claim, response);
        // Add any additional mapping logic here
        return response;
    }

}
