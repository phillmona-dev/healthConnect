package com.medco.HealthConnectProvider.services.impl.provider;

import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.entity.user.Role;
import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.repository.user.RoleRepository;
import com.medco.HealthConnectProvider.repository.user.UserRepository;
import com.medco.HealthConnectProvider.services.mail.EmailService;
import com.medco.HealthConnectProvider.services.providers.ProviderService;
import com.medco.HealthConnectProvider.ui.request.auth.password.providers.ProviderRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.user.SignUpRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.provider.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.provider.PayersNameForProviderResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import com.medco.HealthConnectProvider.utils.SortUtils;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.jpa.domain.Specification;
import com.medco.HealthConnectProvider.specifications.ProviderSpecifications;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.util.Base64;


@Service
@Slf4j
public class ProviderServiceImpl implements ProviderService {

private final Logger logger = LoggerFactory.getLogger(ProviderService.class);
    @Autowired
    ProviderRepository providerRepository;

    @Autowired
    ContractRepository contractRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${file.upload-dir-claims:C:/Users/Administrator/OneDrive/Desktop/MedcoProjects/healthConnect_files}")
    private String uploadDirectory;

    @Value("${file.upload-dir-provider-logos:C:/Users/Administrator/OneDrive/Desktop/MedcoProjects/logos/providers}")
    private String providerLogosDirectory;

    @Override
    public ResponseEntity<ProviderResponse> createProvider(ProviderRequest providerRequest, MultipartFile logo) {

        if (providerRepository.existsByEmail(providerRequest.getEmail())){
            ProviderResponse response = new ProviderResponse();
            response.setStatus("Error: Email is already in use!");
            return ResponseEntity.badRequest().body(response);
        }

        if (providerRepository.existsByProviderName(providerRequest.getProviderName())){
            ProviderResponse response = new ProviderResponse();
            response.setStatus("Error: Provider name is already in use!");
            return ResponseEntity.badRequest().body(response);
        }

        if (providerRepository.existsByTelephone(providerRequest.getTelephone())){
            ProviderResponse response = new ProviderResponse();
            response.setStatus("Error: Phone number is already in use!");
            return ResponseEntity.badRequest().body(response);
        }

        Provider provider = new Provider();
        BeanUtils.copyProperties(providerRequest, provider);
        provider.setStatus(Status.valueOf(providerRequest.getStatus()));

        if (logo != null && !logo.isEmpty()) {
            try {

                File directory = new File(providerLogosDirectory);
                if (!directory.exists()) {
                    directory.mkdirs();
                    log.info("Created directory: {}", providerLogosDirectory);
                }

                String fileName = logo.getOriginalFilename();
                assert fileName != null;
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                String newFileName = "logo_" + UUID.randomUUID().toString() + "." + extension;

                Path logoPath = Paths.get(providerLogosDirectory + "/" + newFileName);
                log.info("Saving new logo to: {}", logoPath);
                Files.write(logoPath, logo.getBytes());

                provider.setLogoPath(newFileName);
                log.info("Set provider logo path to: {}", newFileName);
            } catch (IOException e) {
                log.error("Error uploading logo: {}", e.getMessage(), e);
                ProviderResponse response = new ProviderResponse();
                response.setStatus("Error uploading logo: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        }

        Provider savedProvider = providerRepository.save(provider);
        log.info("Provider created with UUID: {}, status: {}", savedProvider.getProviderUuid(), savedProvider.getStatus());

        Role role = new Role();
        // Truncate the provider name if it's too long to fit in role name
        String providerNameForRole = providerRequest.getProviderName();
        if (providerNameForRole.length() > 40) {
            providerNameForRole = providerNameForRole.substring(0, 40);
        }

        role.setRoleName(providerNameForRole + "_Manager");
        role.setProviderUuid(savedProvider.getProviderUuid());
        role.setRoleDescription("Manages the system for " + providerRequest.getProviderName());
        Role savedRole = roleRepository.save(role);

        createProviderManager(providerRequest, savedRole, savedProvider);

        ProviderResponse providerResponse = new ProviderResponse();
        BeanUtils.copyProperties(savedProvider, providerResponse);
        providerResponse.setStatus(String.valueOf(savedProvider.getStatus()));
        providerResponse.setProviderUuid(savedProvider.getProviderUuid());

        return ResponseEntity.ok(providerResponse);
    }

    @Override
    public ResponseEntity<ByteArrayResource> getProviderLogo(String providerUuid) {
        try {
            Optional<Provider> providerOpt = providerRepository.findByProviderUuid(providerUuid);
            if (providerOpt.isEmpty() || providerOpt.get().getLogoPath() == null || providerOpt.get().getLogoPath().isEmpty()) {
                log.warn("Provider logo not found for UUID: {}", providerUuid);
                return serveDefaultLogo();
            }

            Provider provider = providerOpt.get();
            String logoPath = providerLogosDirectory + "/" + provider.getLogoPath();
            log.debug("Attempting to load logo from path: {}", logoPath);

            File logoFile = new File(logoPath);
            if (!logoFile.exists() || !logoFile.isFile()) {
                log.warn("Logo file does not exist at path: {}", logoPath);
                return serveDefaultLogo();
            }

            Path path = logoFile.toPath();
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

            String contentType = determineContentType(logoPath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(logoFile.length())
                    .body(resource);
        } catch (IOException e) {
            log.error("Error retrieving provider logo: {}", e.getMessage(), e);
            return serveDefaultLogo();
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateProviderStatus(String providerUuid, Status status) {
        Provider provider = providerRepository.findByProviderUuid(providerUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Provider", "providerUuid", providerUuid));

        log.info("Updating provider status: {} from {} to {}",
                provider.getProviderName(), provider.getStatus(), status);

        provider.setStatus(status);
        providerRepository.save(provider);

        String message = String.format("Provider status updated successfully to %s", status);
        return ResponseEntity.ok(new MessageResponse(message));
    }

    private ResponseEntity<ByteArrayResource> serveDefaultLogo() {
        try {

            Resource resource = new ClassPathResource("static/images/default-provider-logo.png");
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
            log.error("Error serving default logo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String determineContentType(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    @Override
    public PagedResponse<ProviderResponse> getProvidersWithFilters(String searchKey, int page, int limit,
                                                                   Status status, String category,
                                                                   String providerName, String tinNumber, String level,
                                                                   String sortBy, String sortDir) {

        if (page > 0) {
            page = page - 1;
        }

        // Validate and sanitize sort parameters
        String validatedSortBy = SortUtils.validateProviderSortField(sortBy);
        String validatedSortDir = SortUtils.validateSortDirection(sortDir);

        // Create sort object
        Sort sort = validatedSortDir.equalsIgnoreCase("asc") ?
                Sort.by(validatedSortBy).ascending() :
                Sort.by(validatedSortBy).descending();

        Pageable pageable = PageRequest.of(page, limit, sort);

        // Build specification for filtering
        Specification<Provider> spec = Specification.where(ProviderSpecifications.isNotDeleted());

        if (searchKey != null && !searchKey.isEmpty()) {
            spec = spec.and(ProviderSpecifications.containsSearchKey(searchKey));
        }

        if (status != null) {
            spec = spec.and(ProviderSpecifications.hasStatus(status));
        }

        if (category != null && !category.isEmpty()) {
            spec = spec.and(ProviderSpecifications.hasCategory(category));
        }

        if (providerName != null && !providerName.isEmpty()) {
            spec = spec.and(ProviderSpecifications.hasProviderName(providerName));
        }

        if (tinNumber != null && !tinNumber.isEmpty()) {
            spec = spec.and(ProviderSpecifications.hasTinNumber(tinNumber));
        }

        if (level != null && !level.isEmpty()) {
            spec = spec.and(ProviderSpecifications.hasLevel(level));
        }

        Page<Provider> providerPage = providerRepository.findAll(spec, pageable);
        List<Provider> providerList = providerPage.getContent();

        // Map to response objects
        List<ProviderResponse> providerResponses = new ArrayList<>();
        for (Provider provider : providerList) {
            ProviderResponse response = new ProviderResponse();

            BeanUtils.copyProperties(provider, response);
            response.setStatus(String.valueOf(provider.getStatus()));

            // Get total contracts for this provider
            Long contractCount = contractRepository.countByProviderProviderUuidAndIsDeleted(
                    provider.getProviderUuid(), false);
            response.setTotalContracts(contractCount);

            // Add logo as base64 if available
            if (provider.getLogoPath() != null && !provider.getLogoPath().isEmpty()) {
                try {
                    String logoPath = providerLogosDirectory + "/" + provider.getLogoPath();
                    File logoFile = new File(logoPath);

                    if (logoFile.exists() && logoFile.isFile()) {
                        byte[] fileContent = Files.readAllBytes(logoFile.toPath());
                        String base64Logo = Base64.getEncoder().encodeToString(fileContent);
                        response.setLogoBase64("data:" + determineContentType(logoPath) + ";base64," + base64Logo);
                    } else {
                        // Set default logo if provider logo doesn't exist
                        setDefaultLogoBase64(response);
                    }
                } catch (IOException e) {
                    log.warn("Could not read logo for provider {}: {}", provider.getProviderUuid(), e.getMessage());
                    // Set default logo on error
                    setDefaultLogoBase64(response);
                }
            } else {
                // Set default logo if provider has no logo path
                setDefaultLogoBase64(response);
            }

            providerResponses.add(response);
        }

        PagedResponse<ProviderResponse> pagedResponse = new PagedResponse<>();
        pagedResponse.setContent(providerResponses);
        pagedResponse.setCurrentPage(providerPage.getNumber() + 1);
        pagedResponse.setPageSize(providerPage.getSize());
        pagedResponse.setTotalElements(providerPage.getTotalElements());
        pagedResponse.setTotalPages(providerPage.getTotalPages());
        pagedResponse.setHasNext(providerPage.hasNext());
        pagedResponse.setHasPrevious(providerPage.hasPrevious());

        return pagedResponse;
    }

    private void setDefaultLogoBase64(ProviderResponse response) {
        try {
            Resource resource = new ClassPathResource("static/images/default-provider-logo.png");
            if (resource.exists()) {
                byte[] fileContent = FileCopyUtils.copyToByteArray(resource.getInputStream());
                String base64Logo = Base64.getEncoder().encodeToString(fileContent);
                response.setLogoBase64("data:image/png;base64," + base64Logo);
            }
        } catch (IOException e) {
            log.warn("Could not read default logo: {}", e.getMessage());
        }
    }

    private void createProviderManager(ProviderRequest providerRequest, Role savedRole, Provider savedProvider) {

        SignUpRequest managerDto = new SignUpRequest();
        managerDto.setEmail(providerRequest.getEmail());
        managerDto.setGender("Male");
        managerDto.setTitle("Mr");
        managerDto.setFirstName("Provider");
        managerDto.setFatherName("Manager");
        managerDto.setGrandFatherName("Default");
        managerDto.setMobilePhone(providerRequest.getTelephone());

        String randomPassword = generateRandomPassword();
        managerDto.setPassword(randomPassword);

        managerDto.setRoleUuid(savedRole.getRoleUuid());
        //managerDto.setProviderUuid(savedProvider.getProviderUuid());
        managerDto.setUserStatus(Status.ACTIVE);

        // Create the user
        try {
            User savedUser = createProviderManagerUser(managerDto, savedProvider);

            // Send welcome email
            String loginUrl = frontendUrl + "/login?newUser=true&email=" + savedUser.getEmail();
            emailService.sendWelcomeEmail(
                    savedUser.getEmail(),
                    savedUser.getFirstName(),
                    randomPassword,
                    savedProvider.getProviderName(),
                    loginUrl
            );
        } catch (Exception e) {
            System.err.println("Failed to create provider manager: " + e.getMessage());
        }
    }

    private User createProviderManagerUser(SignUpRequest managerDto, Provider savedProvide) {
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
        user.setProviderUuid(savedProvide.getProviderUuid());

        return userRepository.save(user);
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
        return sb.toString();
    }

    @Override
    public ResponseEntity<?> updateProvider(String providerUuid, ProviderRequest providerRequest, MultipartFile logo) {
        Provider provider = providerRepository.findByProviderUuid(providerUuid)
                .orElseThrow(() -> new BadRequestException("Can't find Provider with the provided UUID"));

        // Check for duplicate provider name
        if (!provider.getProviderName().equals(providerRequest.getProviderName()) &&
                providerRepository.existsByProviderName(providerRequest.getProviderName())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Provider name is already in use!"));
        }

        BeanUtils.copyProperties(providerRequest, provider);

        // Set status if provided, otherwise keep existing
        if (providerRequest.getStatus() != null && !providerRequest.getStatus().isEmpty()) {
            provider.setStatus(Status.valueOf(providerRequest.getStatus()));
        }

        // Update logo if provided
        if (logo != null && !logo.isEmpty()) {
            try {
                // Ensure directory exists
                File directory = new File(providerLogosDirectory);
                if (!directory.exists()) {
                    directory.mkdirs();
                    log.info("Created directory: {}", providerLogosDirectory);
                }

                // Delete old logo if exists
                if (provider.getLogoPath() != null && !provider.getLogoPath().isEmpty()) {
                    Path oldLogoPath = Paths.get(providerLogosDirectory + "/" + provider.getLogoPath());
                    try {
                        Files.deleteIfExists(oldLogoPath);
                        log.info("Deleted old logo: {}", oldLogoPath);
                    } catch (IOException e) {
                        log.warn("Failed to delete old logo: {}", e.getMessage());
                    }
                }

                String fileName = logo.getOriginalFilename();
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                String newFileName = "logo_" + UUID.randomUUID().toString() + "." + extension;

                // Save the new logo
                Path newLogoPath = Paths.get(providerLogosDirectory + "/" + newFileName);
                log.info("Saving new logo to: {}", newLogoPath);
                Files.write(newLogoPath, logo.getBytes());

                provider.setLogoPath(newFileName);
                log.info("Updated provider logo path to: {}", newFileName);
            } catch (IOException e) {
                log.error("Error uploading logo: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new MessageResponse("Error uploading logo: " + e.getMessage()));
            }
        }

        providerRepository.save(provider);

        ProviderResponse providerResponse = new ProviderResponse();
        BeanUtils.copyProperties(provider, providerResponse);
        providerResponse.setStatus(String.valueOf(provider.getStatus()));

        return ResponseEntity.ok(providerResponse);
    }

    @Override
    public ProviderResponse getProvider(String providerUuid) {
        Provider provider = providerRepository.findByProviderUuid(providerUuid)
                .orElseThrow(() -> new RuntimeException("provider not found with UUID:" + providerUuid));

        ProviderResponse providerResponse = new ProviderResponse();
        BeanUtils.copyProperties(provider, providerResponse);
        providerResponse.setStatus(String.valueOf(provider.getStatus()));

        // Add logo as base64 if available
        if (provider.getLogoPath() != null && !provider.getLogoPath().isEmpty()) {
            try {
                String logoPath = providerLogosDirectory + "/" + provider.getLogoPath();
                File logoFile = new File(logoPath);

                if (logoFile.exists() && logoFile.isFile()) {
                    byte[] fileContent = Files.readAllBytes(logoFile.toPath());
                    String base64Logo = Base64.getEncoder().encodeToString(fileContent);
                    providerResponse.setLogoBase64("data:" + determineContentType(logoPath) + ";base64," + base64Logo);
                } else {
                    // Set default logo if provider logo doesn't exist
                    setDefaultLogoBase64(providerResponse);
                }
            } catch (IOException e) {
                log.warn("Could not read logo for provider {}: {}", provider.getProviderUuid(), e.getMessage());
                // Set default logo on error
                setDefaultLogoBase64(providerResponse);
            }
        } else {
            // Set default logo if provider has no logo path
            setDefaultLogoBase64(providerResponse);
        }

        return providerResponse;
    }

    @Override
    public List<ProviderResponse> getProviders(String searchKey, int page, int limit) {
        //UserPrincipal userDetails = AuthUtil.getCurrentUser();

        if (page > 0)
            page = page - 1;
        Pageable pageRequest = PageRequest.of(page, limit, Sort.by("id").descending());
        Page<Provider> provider;
        if (searchKey != null)
            provider = providerRepository.findAllByProviderNameContaining( searchKey, pageRequest);
        else provider = providerRepository.findAll( pageRequest);
        long totalPages = provider.getTotalPages();
        List<Provider> providerList = provider.getContent();

        List<ProviderResponse> providerResponse = new ArrayList<>();
        for (Provider p : providerList) {

            ProviderResponse pr = new ProviderResponse();
            if (providerResponse.size() == 0)
                pr.setTotalPages(totalPages);
            BeanUtils.copyProperties(p, pr);
            providerResponse.add(pr);
        }
        return providerResponse;
    }

    @Override
    public ResponseEntity<?> deleteProvider(String providerUuid) {
        Optional<Provider> provider = Optional.ofNullable(providerRepository.findByProviderUuid(providerUuid)
                .orElseThrow(() -> new BadRequestException("can't find Hospital with the provided Id")));

        provider.get().setDeleted(true);
        providerRepository.save(provider.get());
        return ResponseEntity.ok(new MessageResponse("Provider soft deleted successfully!"));
    }

    @Override
    public List<ProviderResponse> getAvailableProvidersForPayerNotInContract(String payerUuid, String searchKey, int page, int limit) {
        if (page > 0) {
            page = page - 1;
        }

        Pageable pageRequest = PageRequest.of(page, limit, Sort.by("id").descending());
        Page<Provider> providerPage;

        // Find providers not already in contract with this payer
        if (searchKey != null && !searchKey.isEmpty()) {

            providerPage = providerRepository.findAvailableProvidersForPayerWithSearch(payerUuid, searchKey, pageRequest);

        } else {
            providerPage = providerRepository.findAvailableProvidersForPayerNotInContract(payerUuid, pageRequest);
        }

        List<Provider> providerList = providerPage.getContent();
        List<ProviderResponse> providerResponses = new ArrayList<>();

        for (Provider provider : providerList) {
            ProviderResponse response = new ProviderResponse();
            BeanUtils.copyProperties(provider, response);

            // Add total pages to first response
            if (providerResponses.isEmpty()) {
                response.setTotalPages((int) providerPage.getTotalPages());
            }

            providerResponses.add(response);
        }

        return providerResponses;
    }

    @Override
    public List<PayersNameForProviderResponse> getPayersNameForProvider(String providerUuid, String searchKey) {
        List<ContractHeader> contractList = searchKey != null ? getPayerNameWithSearch(providerUuid,searchKey) : getPayers(providerUuid);

        return contractList.stream()
                .map(contract -> {
                    var response = new PayersNameForProviderResponse();
                    BeanUtils.copyProperties(contract, response);

                    return response;
                }).collect(Collectors.toList());
    }

    private List<ContractHeader> getPayers(String providerUuid) {
        return contractRepository.findAllByProviderProviderUuidAndIsDeletedGroupByPayerPayerUuid(providerUuid, false);
    }

    private List<ContractHeader> getPayerNameWithSearch(String providerUuid, String searchKey) {
        return contractRepository.findAllByProviderProviderUuidAndIsDeletedGroupByPayerPayerUuidAndContainingName(providerUuid, false, searchKey);
    }

}
