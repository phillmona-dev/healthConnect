package com.medco.HealthConnectProvider.services.impl.packageCategory;

import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategory;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.packageCategory.PackageCategoryRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.ServiceCategoryMappingRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public ResponseEntity<PackageCategoryResponse> createPackageCategory(PackageCategoryRequest request) {
        // Get current logged-in user's payer UUID
        String payerUuid = SecurityUtils.getAuthenticatedUser().getPayerUuid();
        if (payerUuid == null || payerUuid.isEmpty()) {
            throw new BadRequestException("User must be associated with a payer to create package categories");
        }

        log.info("Creating package category for payer: {}", payerUuid);

        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "payerUuid", payerUuid);
        }

        // Check if category code is unique for this payer
        if (packageCategoryRepository.existsByCategoryCodeAndPayerAndIsDeletedFalse(request.getCategoryCode(), payer)) {
            throw new BadRequestException("Category code already exists for this payer: " + request.getCategoryCode());
        }

        // Check if category name is unique for this payer
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

        // Check if category code is unique (excluding current category)
        PackageCategory existingByCode = packageCategoryRepository.findByCategoryCodeAndPayer(request.getCategoryCode(), packageCategory.getPayer())
                .orElse(null);
        if (existingByCode != null && !existingByCode.getCategoryUuid().equals(categoryUuid)) {
            throw new BadRequestException("Category code already exists for this payer: " + request.getCategoryCode());
        }

        // Check if category name is unique (excluding current category)
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
    public PagedResponse<PackageCategoryResponse> getPackageCategories(String searchKey, String status, int page, int size) {
        String payerUuid = SecurityUtils.getAuthenticatedUser().getPayerUuid();
        if (payerUuid == null || payerUuid.isEmpty()) {
            throw new BadRequestException("User must be associated with a payer to view package categories");
        }

        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "payerUuid", payerUuid);
        }

        Status statusEnum = status != null ? Status.valueOf(status.toUpperCase()) : Status.ACTIVE;
        Pageable pageable = PageRequest.of(page, size, Sort.by("categoryName").ascending());

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
                categoryPage.getNumber(),
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

        // Check if category has any service mappings
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
        // Get current logged-in user's payer UUID
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
        // Get current logged-in user's payer UUID
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
