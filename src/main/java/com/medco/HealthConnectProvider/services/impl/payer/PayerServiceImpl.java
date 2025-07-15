package com.medco.HealthConnectProvider.services.impl.payer;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.entity.user.Privilege;
import com.medco.HealthConnectProvider.entity.user.Role;
import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.claims.ClaimRepository;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.repository.user.PrivilegeRepository;
import com.medco.HealthConnectProvider.repository.user.RoleRepository;
import com.medco.HealthConnectProvider.repository.user.UserRepository;
import com.medco.HealthConnectProvider.services.mail.EmailService;
import com.medco.HealthConnectProvider.services.payer.PayerService;
import com.medco.HealthConnectProvider.services.user.UserService;
import com.medco.HealthConnectProvider.ui.request.auth.password.payer.PayerRequest;
import com.medco.HealthConnectProvider.ui.request.claims.ClaimReviewRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.claims.ClaimResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PayerImportResponse;
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
import org.apache.poi.ss.usermodel.*;
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
import org.springframework.util.StringUtils;
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
import java.util.stream.Collectors;

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
    private final PrivilegeRepository privilegeRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ProviderRepository providerRepository;

    @Value("${file.upload-dir-payer-logos:C:/Users/Administrator/OneDrive/Desktop/MedcoProjects/logos/payers}")
    private String payerLogosDirectory;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public PayerServiceImpl(PayerRepository payerRepository, RoleRepository roleRepository, UserService userService, ContractRepository contractRepository, PrivilegeRepository privilegeRepository) {
        this.payerRepository = payerRepository;
        this.roleRepository = roleRepository;
        this.userService = userService;
        this.contractRepository = contractRepository;
        this.privilegeRepository = privilegeRepository;
    }


    @Transactional
    @Override
    public PayerResponse createPayer(@Valid PayerRequest payerRequest, MultipartFile logo) {

        log.info("Starting payer creation process for: {}", payerRequest.getPayerName());

        if (payerRequest.getEmail() != null && !payerRequest.getEmail().trim().isEmpty()) {
            if (payerRepository.existsByEmail(payerRequest.getEmail())) {
                log.warn("Duplicate payer email: {}", payerRequest.getEmail());
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Error: Duplicated Payer Email is not allowed.");
            }
        }

        if (payerRepository.existsByTelephone(payerRequest.getTelephone())) {
            log.warn("Duplicate payer telephone: {}", payerRequest.getTelephone());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Error: Duplicated Payer Telephone is not allowed.");
        }

        if (payerRepository.existsByPayerName(payerRequest.getPayerName())) {
            log.warn("Duplicate payer name: {}", payerRequest.getPayerName());
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
                    log.info("Created directory: {}", payerLogosDirectory);

                }

                String fileName = logo.getOriginalFilename();
                assert fileName != null;
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                String newFileName = "logo_" + UUID.randomUUID().toString() + "." + extension;

                Path path = Paths.get(payerLogosDirectory + "/" + newFileName);
                Files.write(path, logo.getBytes());
                log.info("Saved logo to: {}", path);

                payer.setLogoPath(newFileName);
            } catch (IOException e) {
                log.error("Error uploading logo: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error uploading logo: " + e.getMessage());
            }
        }

        Payer savedPayer = payerRepository.save(payer);
        log.info("Payer created with UUID: {}, status: {}", savedPayer.getPayerUuid(), savedPayer.getStatus());

        Role role = new Role();
        String payerNameForRole = payerRequest.getPayerName();
        if (payerNameForRole.length() > 35) {
            payerNameForRole = payerNameForRole.substring(0, 35);
        }
        role.setRoleName("PA_" + payerNameForRole + "_Manager");
        role.setPayerUuid(savedPayer.getPayerUuid());
        role.setRoleDescription("Manages the system for " + payerRequest.getPayerName());
        Role savedRole = roleRepository.save(role);
        log.info("Created role: {} with UUID: {}", savedRole.getRoleName(), savedRole.getRoleUuid());

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

        log.info("Payer creation process completed for: {}", savedPayer.getPayerName());
        return payerResponse;
    }

    @Override
    @Transactional
    public PayerResponse updatePayer(String payerUuid, PayerRequest payerRequest, MultipartFile logo) {
        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        List<Payer> payerName = payerRepository
                .findAllByPayerName(payerRequest.getPayerName());

        if (payer == null)
            throw new ResourceNotFoundException("Payer", "PayerUuid", payerUuid);

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

        if (logo != null && !logo.isEmpty()) {
            try {

                File directory = new File(payerLogosDirectory);
                if (!directory.exists()) {
                    directory.mkdirs();
                    logger.info("Created directory: {}", payerLogosDirectory);
                }

                if (payer.getLogoPath() != null && !payer.getLogoPath().isEmpty()) {
                    Path oldLogoPath = Paths.get(payerLogosDirectory + "/" + payer.getLogoPath());
                    try {
                        Files.deleteIfExists(oldLogoPath);
                        logger.info("Deleted old logo: {}", oldLogoPath);
                    } catch (IOException e) {
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

            Resource resource = new ClassPathResource("static/images/default-payer-logo.png");
            if (resource.exists()) {
                ByteArrayResource byteResource = new ByteArrayResource(
                        FileCopyUtils.copyToByteArray(resource.getInputStream()));

                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .contentLength(resource.contentLength())
                        .body(byteResource);
            }

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

    @Override
    public ResponseEntity<PagedResponse<ProviderResponse>> getProvidersWithContract(String payerUuid, int page, int size, String sortBy, String sortDir, String search) {
        log.info("Fetching providers with contract for payer UUID: {}", payerUuid);

        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "UUID", payerUuid);
        }

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() :
                Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<Provider> providerPage;
        if (StringUtils.hasText(search)) {
            providerPage = providerRepository.findProvidersWithContractByPayer(payerUuid, search, pageable);
        } else {
            providerPage = providerRepository.findProvidersWithContractByPayer(payerUuid, pageable);
        }

        List<ProviderResponse> providerResponses = providerPage.getContent().stream()
                .map(this::mapToProviderResponse)
                .collect(Collectors.toList());

        PagedResponse<ProviderResponse> pagedResponse = new PagedResponse<>(
                providerResponses,
                page,
                size,
                providerPage.getTotalElements(),
                providerPage.getTotalPages(),
                providerPage.hasNext(),
                providerPage.hasPrevious()
        );

        log.info("Successfully fetched {} providers with contract for payer UUID: {}", providerResponses.size(), payerUuid);
        return ResponseEntity.ok(pagedResponse);
    }

    @Override
    @Transactional
    public PayerImportResponse importPayersFromExcel(MultipartFile file) throws IOException {
        List<PayerResponse> importedPayers = new ArrayList<>();
        List<String> skippedPayers = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            if (!rows.hasNext()) {
                errors.add("Excel file is empty");
                return new PayerImportResponse(importedPayers, skippedPayers, errors, 0, 0);
            }

            Row headerRow = rows.next();
            Map<String, Integer> headerMap = createHeaderMap(headerRow);

            if (!validateHeaders(headerMap)) {
                errors.add("Missing required headers in the Excel file");
                return new PayerImportResponse(importedPayers, skippedPayers, errors, 0, 0);
            }

            int rowNumber = 1;
            while (rows.hasNext()) {
                rowNumber++;
                Row currentRow = rows.next();

                try {
                    Payer payer = createPayerFromRow(currentRow, headerMap);

                    if (payer != null) {
                        if (isDuplicatePayer(payer)) {
                            skippedPayers.add("Row " + rowNumber + ": " + payer.getPayerName() + " (duplicate name or phone)");
                        } else {
                            Payer savedPayer = payerRepository.save(payer);

                            Role defaultRole = createDefaultRoleForPayer(savedPayer);
                            User defaultUser = createDefaultUserForPayer(savedPayer, defaultRole);

                            importedPayers.add(mapToPayerResponse(savedPayer));
                            logger.info("Payer imported with UUID: {}, status: {}", savedPayer.getPayerUuid(), savedPayer.getStatus());
                            logger.info("Default user created for payer: {}, User UUID: {}", savedPayer.getPayerName(), defaultUser.getUserUuid());
                        }
                    } else {
                        skippedPayers.add("Row " + rowNumber + ": Invalid or missing required data");
                    }
                } catch (Exception e) {
                    logger.error("Error processing row {}: ", rowNumber, e);
                    errors.add("Error in row " + rowNumber + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Error processing Excel file: ", e);
            errors.add("Error processing Excel file: " + e.getMessage());
        }

        int successFullImports = importedPayers.size();
        int skippedImports = skippedPayers.size();

        logger.info("Import process completed. Imported: {}, Skipped: {}, Errors: {}",
                importedPayers.size(), skippedPayers.size(), errors.size());

        return new PayerImportResponse(importedPayers, skippedPayers, errors, successFullImports, skippedImports);

    }

    private User createDefaultUserForPayer(Payer payer, Role role) {
        logger.info("Creating default user for payer: {}", payer.getPayerName());

        User user = new User();

        user.setUserUuid(UUID.randomUUID().toString());
        user.setEmail(generateDefaultEmail(payer));

        String payerName = payer.getPayerName();

        user.setPassword(passwordEncoder.encode(payerName));
        user.setTitle("Mr");
        user.setFirstName(payer.getPayerName());
        user.setFatherName("User");
        user.setGrandFatherName("Manager");
        user.setGender("male");
        user.setMobilePhone(payer.getTelephone());
        user.setUserStatus(Status.ACTIVE);
        user.setUserType("PAYER_ADMIN");
        user.setStatus(Status.ACTIVE);
        user.setPayerUuid(payer.getPayerUuid());
        user.setPayer(payer);
        user.setRole(role);
        user.setFirstTimeLogin(true);
        user.setCreatedDate(new Date());
        user.setLastModifiedDate(new Date());
        user.setCreatedBy("SYSTEM");
        user.setLastModifiedBy("SYSTEM");

        logger.debug("Default user details: UUID={}, Email={}, UserType={}, Role={}",
                user.getUserUuid(), user.getEmail(), user.getUserType(), role.getRoleName());

        User savedUser = userRepository.save(user);
        logger.info("Default user created successfully for payer: {}. User UUID: {}",
                payer.getPayerName(), savedUser.getUserUuid());

        // TODO: Send email to the user with login credentials
        logger.info("TODO: Send welcome email to user: {}", savedUser.getEmail());

        return savedUser;
    }

    private String generateDefaultEmail(Payer payer) {
        String sanitizedName = payer.getPayerName().toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .replaceAll("^[^a-z]", "a");

        if (sanitizedName.isEmpty()) {
            sanitizedName = "user";
        }

        String baseEmail = sanitizedName + "@gmail.com";
        String email = baseEmail;
        int counter = 1;
        while (userRepository.existsByEmail(email)) {
            email = sanitizedName + counter + "@gmail.com";
            counter++;
        }
        return email;
    }

//    private String generateRandomPassword() {
//        return UUID.randomUUID().toString().substring(0, 8);
//    }

    private Role createDefaultRoleForPayer(Payer payer) {

        Role role = new Role();
        String payerNameForRole = payer.getPayerName();
        if (payerNameForRole.length() > 35) {
            payerNameForRole = payerNameForRole.substring(0, 35);
        }

        role.setRoleName("PA_" + payerNameForRole + "_Manager");
        role.setPayerUuid(payer.getPayerUuid());
        role.setRoleDescription("Manages the system for " + payer.getPayerName());
        role.setRoleUuid(UUID.randomUUID().toString());

        Privilege createEmployeesPrivilege = privilegeRepository.findByPrivilegeName("Create Employees")
                .orElseThrow(() -> new RuntimeException("Create Employees privilege not found"));

        List<Privilege> privileges = new ArrayList<>();
        privileges.add(createEmployeesPrivilege);
        role.setPrivileges(privileges);

        Role savedRole = roleRepository.save(role);
        log.info("Default role created for payer: {}, Role UUID: {}, with Create Employees privilege",
                payer.getPayerName(), savedRole.getRoleUuid());

        return savedRole;

    }

    private Map<String, Integer> createHeaderMap(Row headerRow) {
        Map<String, Integer> headerMap = new HashMap<>();
        for (Cell cell : headerRow) {
            String headerName = getCellValueAsString(cell).trim().toLowerCase();
            if (!headerName.isEmpty()) {
                headerMap.put(headerName, cell.getColumnIndex());
            }
        }
        return headerMap;
    }

    private boolean validateHeaders(Map<String, Integer> headerMap) {
        return headerMap.containsKey("payer name") &&
                headerMap.containsKey("phone") &&
                headerMap.containsKey("subcity") &&
                headerMap.containsKey("email") &&
                headerMap.containsKey("tin number");
    }

    private Payer createPayerFromRow(Row row, Map<String, Integer> headerMap) {
        String payerName = getStringCellValue(row, headerMap, "payer name");
        String phone = getStringCellValue(row, headerMap, "phone");
        String address1 = getStringCellValue(row, headerMap, "woreda");
        String address2 = getStringCellValue(row, headerMap, "subcity");
        String address3 = getStringCellValue(row, headerMap, "city");
        String state = getStringCellValue(row, headerMap, "state");
        String email = getStringCellValue(row, headerMap, "email");
        Long tinNumber = getLongCellValue(row, headerMap, "tin number");

        if (payerName == null || payerName.trim().isEmpty() ||
                phone == null || phone.trim().isEmpty() ||
                address2 == null || address2.trim().isEmpty()) {
            log.warn("Skipping row due to missing mandatory fields (Payer Name, Phone, or subcity)");
            return null;
        }

        // Process phone number
        phone = processPhoneNumber(phone);

        Payer payer = new Payer();

        payer.setPayerName(payerName);
        payer.setTelephone(phone);
        payer.setAddress1(address1);
        payer.setAddress2(address2);
        payer.setAddress3(address3);
        payer.setState(state);
        payer.setTinNumber(tinNumber);

        if (email == null || email.trim().isEmpty()) {
            email = payerName.replaceAll("\\s+", "").toLowerCase() + "@gmail.com";
        }
        payer.setEmail(email);

        payer.setPayerUuid(UUID.randomUUID().toString());
        payer.setRegistrationDate(new Date());
        payer.setStatus(Status.ACTIVE);

        return payer;
    }

    private String processPhoneNumber(String phone) {
        if (phone == null) return null;
        phone = phone.replaceAll("\\D", "");

        if (!phone.startsWith("+251")) {
            phone = "+251" + phone;
        }

        if (phone.length() > 13) {
            phone = phone.substring(0, 13);
        }

        return phone;
    }

    private boolean isDuplicatePayer(Payer payer) {
        return payerRepository.existsByPayerNameOrTelephone(payer.getPayerName(), payer.getTelephone());
    }

    private PayerResponse mapToPayerResponse(Payer payer) {
        PayerResponse response = new PayerResponse();
        BeanUtils.copyProperties(payer, response);
        return response;
    }

    private String getStringCellValue(Row row, Map<String, Integer> headerMap, String headerName) {
        Integer columnIndex = headerMap.get(headerName.toLowerCase());
        if (columnIndex == null) return null;
        Cell cell = row.getCell(columnIndex);
        return getCellValueAsString(cell);
    }

    private Long getLongCellValue(Row row, Map<String, Integer> headerMap, String headerName) {
        Integer columnIndex = headerMap.get(headerName.toLowerCase());
        if (columnIndex == null) return null;
        Cell cell = row.getCell(columnIndex);
        return getCellValueAsLong(cell);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                yield String.format("%.0f", cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private Long getCellValueAsLong(Cell cell) {

        if (cell == null) return null;
        switch (cell.getCellType()) {
            case NUMERIC:
                return (long) cell.getNumericCellValue();
            case STRING:
                try {
                    return Long.parseLong(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }

    }

    private ProviderResponse mapToProviderResponse(Provider provider) {

        ProviderResponse response = new ProviderResponse();
        BeanUtils.copyProperties(provider, response);

        response.setTotalContracts((long) provider.getContractHeaders().size());

        return response;

    }

    private ClaimResponse mapClaimToClaimResponse(Claim claim) {
        ClaimResponse response = new ClaimResponse();
        BeanUtils.copyProperties(claim, response);
        return response;
    }

}
