package com.medco.HealthConnectProvider.services.impl.packageCategory;

import com.medco.HealthConnectProvider.config.ExternalApiConfig;
import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategory;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.packageCategory.PackageCategoryRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.ServiceCategoryMappingRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.ui.response.packageCategory.ExternalPackageCategoryResponse;
import com.medco.HealthConnectProvider.utils.packageCategory.PackageCategorySpecifications;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import com.medco.HealthConnectProvider.services.packageCategory.PackageCategoryService;
import com.medco.HealthConnectProvider.ui.request.packageCategory.PackageCategoryRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.PackageCategoryResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PackageCategoryServiceImpl implements PackageCategoryService {

    private final PackageCategoryRepository packageCategoryRepository;
    private final PayerRepository payerRepository;
    private final ServiceCategoryMappingRepository serviceCategoryMappingRepository;

    private final ExternalApiConfig externalApiConfig;
    private final RestTemplate restTemplate;

    @Override
    public ResponseEntity<PackageCategoryResponse> createPackageCategory(PackageCategoryRequest request) {

        String payerUuid = SecurityUtils.getAuthenticatedUser().getPayerUuid();
        if (payerUuid == null || payerUuid.isEmpty()) {
            throw new BadRequestException("User must be associated with a payer to create package categories");
        }

        log.info("Creating package category for payer: {}", payerUuid);

        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "payerUuid", payerUuid);
        }

        if (packageCategoryRepository.existsByCategoryCodeAndPayerAndIsDeletedFalse(request.getCategoryCode(), payer)) {
            throw new BadRequestException("Category code already exists for this payer: " + request.getCategoryCode());
        }

        if (packageCategoryRepository.existsByCategoryNameAndPayerAndIsDeletedFalse(request.getCategoryName(), payer)) {
            throw new BadRequestException("Category name already exists for this payer: " + request.getCategoryName());
        }

        PackageCategory packageCategory = PackageCategory.builder()
                .categoryName(request.getCategoryName())
                .categoryCode(request.getCategoryCode().toUpperCase())
                .description(request.getDescription())
                .status(Status.valueOf(request.getStatus()))
                .payer(payer)
                .build();

        PackageCategory savedCategory = packageCategoryRepository.save(packageCategory);
        log.info("Package category created successfully with UUID: {}", savedCategory.getCategoryUuid());

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(savedCategory));
    }

    @Override
    public ResponseEntity<PackageCategoryResponse> updatePackageCategory(String categoryUuid, PackageCategoryRequest request) {
        log.info("Updating package category: {}", categoryUuid);

        PackageCategory packageCategory = packageCategoryRepository.findByCategoryUuid(categoryUuid)
                .orElseThrow(() -> new ResourceNotFoundException("PackageCategory", "categoryUuid", categoryUuid));

        PackageCategory existingByCode = packageCategoryRepository.findByCategoryCodeAndPayer(request.getCategoryCode(), packageCategory.getPayer())
                .orElse(null);
        if (existingByCode != null && !existingByCode.getCategoryUuid().equals(categoryUuid)) {
            throw new BadRequestException("Category code already exists for this payer: " + request.getCategoryCode());
        }

        if (packageCategoryRepository.existsByCategoryNameAndPayerAndIsDeletedFalse(request.getCategoryName(), packageCategory.getPayer())) {
            PackageCategory existingByName = packageCategoryRepository.findByPayerAndStatusOrderByCategoryNameAsc(
                            packageCategory.getPayer(), Status.ACTIVE).stream()
                    .filter(cat -> cat.getCategoryName().equals(request.getCategoryName()) && !cat.getCategoryUuid().equals(categoryUuid))
                    .findFirst().orElse(null);
            if (existingByName != null) {
                throw new BadRequestException("Category name already exists for this payer: " + request.getCategoryName());
            }
        }

        packageCategory.setCategoryName(request.getCategoryName());
        packageCategory.setCategoryCode(request.getCategoryCode().toUpperCase());
        packageCategory.setDescription(request.getDescription());
        packageCategory.setStatus(Status.valueOf(request.getStatus()));

        PackageCategory updatedCategory = packageCategoryRepository.save(packageCategory);
        log.info("Package category updated successfully: {}", categoryUuid);

        return ResponseEntity.ok(mapToResponse(updatedCategory));

    }

    @Override
    @Transactional(readOnly = true)
    public PackageCategoryResponse getPackageCategory(String categoryUuid) {
        PackageCategory packageCategory = packageCategoryRepository.findByCategoryUuid(categoryUuid)
                .orElseThrow(() -> new ResourceNotFoundException("PackageCategory", "categoryUuid", categoryUuid));

        return mapToResponse(packageCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PackageCategoryResponse> getPackageCategories(String searchKey, String status,String payerUuid, int page, int size) {
//        String payerUuid = SecurityUtils.getAuthenticatedUser().getPayerUuid();
//        if (payerUuid == null || payerUuid.isEmpty()) {
//            throw new BadRequestException("User must be associated with a payer to view package categories");
//        }

        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "payerUuid", payerUuid);
        }

        Status statusEnum = status != null ? Status.valueOf(status.toUpperCase()) : Status.ACTIVE;

        int zeroBasedPage = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(zeroBasedPage, size, Sort.by("categoryName").ascending());

        Specification<PackageCategory> spec = PackageCategorySpecifications.withPayer(payer)
                .and(PackageCategorySpecifications.withStatus(statusEnum))
                .and(PackageCategorySpecifications.notDeleted())
                .and(PackageCategorySpecifications.withSearchKey(searchKey));

        Page<PackageCategory> categoryPage = packageCategoryRepository.findAll(spec, pageable);

        List<PackageCategoryResponse> responses = categoryPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                responses,
                categoryPage.getNumber() + 1,
                categoryPage.getSize(),
                categoryPage.getTotalElements(),
                categoryPage.getTotalPages(),
                categoryPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageCategoryResponse> getActivePackageCategories() {
        String payerUuid = SecurityUtils.getAuthenticatedUser().getPayerUuid();
        if (payerUuid == null || payerUuid.isEmpty()) {
            throw new BadRequestException("User must be associated with a payer to view package categories");
        }

        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "payerUuid", payerUuid);
        }

        List<PackageCategory> categories = packageCategoryRepository.findByPayerAndStatusOrderByCategoryNameAsc(
                payer, Status.ACTIVE);

        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<String> deactivatePackageCategory(String categoryUuid) {
        log.info("Deactivating package category: {}", categoryUuid);

        PackageCategory packageCategory = packageCategoryRepository.findByCategoryUuid(categoryUuid)
                .orElseThrow(() -> new ResourceNotFoundException("PackageCategory", "categoryUuid", categoryUuid));

        packageCategory.setStatus(Status.INACTIVE);
        packageCategoryRepository.save(packageCategory);

        log.info("Package category deactivated successfully: {}", categoryUuid);
        return ResponseEntity.ok("Package category deactivated successfully");
    }

    @Override
    public ResponseEntity<String> activatePackageCategory(String categoryUuid) {
        log.info("Activating package category: {}", categoryUuid);

        PackageCategory packageCategory = packageCategoryRepository.findByCategoryUuid(categoryUuid)
                .orElseThrow(() -> new ResourceNotFoundException("PackageCategory", "categoryUuid", categoryUuid));

        packageCategory.setStatus(Status.ACTIVE);
        packageCategoryRepository.save(packageCategory);

        log.info("Package category activated successfully: {}", categoryUuid);
        return ResponseEntity.ok("Package category activated successfully");
    }

    @Override
    public ResponseEntity<String> deletePackageCategory(String categoryUuid) {
        log.info("Deleting package category: {}", categoryUuid);

        PackageCategory packageCategory = packageCategoryRepository.findByCategoryUuid(categoryUuid)
                .orElseThrow(() -> new ResourceNotFoundException("PackageCategory", "categoryUuid", categoryUuid));

        long serviceCount = serviceCategoryMappingRepository.countActiveServicesByCategory(packageCategory);
        if (serviceCount > 0) {
            throw new BadRequestException("Cannot delete category with active service mappings. Please remove all services first.");
        }

        packageCategory.setDeleted(true);
        packageCategoryRepository.save(packageCategory);

        log.info("Package category deleted successfully: {}", categoryUuid);
        return ResponseEntity.ok("Package category deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCategoryCodeUnique(String categoryCode) {
        String payerUuid = SecurityUtils.getAuthenticatedUser().getPayerUuid();
        if (payerUuid == null || payerUuid.isEmpty()) {
            throw new BadRequestException("User must be associated with a payer to validate category codes");
        }

        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "payerUuid", payerUuid);
        }

        return !packageCategoryRepository.existsByCategoryCodeAndPayerAndIsDeletedFalse(categoryCode.toUpperCase(), payer);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCategoryNameUnique(String categoryName) {
        String payerUuid = SecurityUtils.getAuthenticatedUser().getPayerUuid();
        if (payerUuid == null || payerUuid.isEmpty()) {
            throw new BadRequestException("User must be associated with a payer to validate category names");
        }

        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "payerUuid", payerUuid);
        }

        return !packageCategoryRepository.existsByCategoryNameAndPayerAndIsDeletedFalse(categoryName, payer);
    }

    @Override
    public List<ExternalPackageCategoryResponse> getEligiblePackages(String insuredUuid) {
        log.info("[Package Service] Starting to fetch eligible packages for insured: {}", insuredUuid);
        log.debug("[Package Service] Building request for external API...");

        String url = String.format("%s/api/payer/claimconnect/package/insured-eligible-packages-dropdown/%s",
                externalApiConfig.getExternalApiBaseUrl(),
                insuredUuid);
        log.debug("[Package Service] Constructed API URL: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", externalApiConfig.getApiKey());
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        log.debug("[Package Service] Prepared request headers");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        log.debug("[Package Service] Created HTTP entity with headers");

        try {
            log.info("[Package Service] Making GET request to external API for insured {}", insuredUuid);
            log.debug("[Package Service] Request details - URL: {}, Method: GET", url);

            long startTime = System.currentTimeMillis();
            ResponseEntity<ExternalPackageCategoryResponse[]> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            ExternalPackageCategoryResponse[].class);
            long duration = System.currentTimeMillis() - startTime;

            log.debug("[Package Service] Received response in {} ms", duration);
            log.debug("[Package Service] Response status code: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful()) {
                if (response.getBody() != null) {
                    log.info("[Package Service] Successfully retrieved {} packages for insured {}",
                            response.getBody().length, insuredUuid);
                    log.debug("[Package Service] First package details (if available): {}",
                            response.getBody().length > 0 ? response.getBody()[0] : "No packages found");
                    return Arrays.asList(response.getBody());
                } else {
                    log.warn("[Package Service] Received successful response but with null body for insured {}", insuredUuid);
                }
            }

            log.error("[Package Service] Failed to fetch packages. Status: {}, Response: {}",
                    response.getStatusCode(),
                    response.getBody() != null ? "Body present" : "Empty body");
            throw new RuntimeException("Failed to fetch packages from external system. Status: " + response.getStatusCode());

        } catch (RestClientException e) {
            log.error("[Package Service] Exception occurred while calling external API for insured {}: {}",
                    insuredUuid,
                    e.getClass().getSimpleName(),
                    e.getMessage());
            log.debug("[Package Service] Stack trace for debugging:", e);
            throw new RuntimeException("Error calling external package API: " + e.getMessage(), e);
        } finally {
            log.info("[Package Service] Completed package fetch operation for insured {}", insuredUuid);
        }
    }

    private PackageCategoryResponse mapToResponse(PackageCategory category) {
        long totalServices = serviceCategoryMappingRepository.countActiveServicesByCategory(category);

        return PackageCategoryResponse.builder()
                .categoryUuid(category.getCategoryUuid())
                .categoryName(category.getCategoryName())
                .categoryCode(category.getCategoryCode())
                .description(category.getDescription())
                .status(category.getStatus().toString())
                .payerName(category.getPayer().getPayerName())
                .payerUuid(category.getPayer().getPayerUuid())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .createdBy(category.getCreatedBy())
                .totalServices(totalServices)
                .totalContracts(category.getCategoryLimits().size())
                .build();
    }
}
