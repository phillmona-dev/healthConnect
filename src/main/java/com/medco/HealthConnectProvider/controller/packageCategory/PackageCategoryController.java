package com.medco.HealthConnectProvider.controller.packageCategory;

import com.medco.HealthConnectProvider.services.packageCategory.PackageCategoryService;
import com.medco.HealthConnectProvider.ui.request.packageCategory.PackageCategoryRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.ExternalPackageCategoryResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.PackageCategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/package-categories")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Package Categories", description = "APIs for managing service package categories")
@RequiredArgsConstructor
@Slf4j
public class PackageCategoryController {

    private final PackageCategoryService packageCategoryService;

    @PostMapping
    @Operation(summary = "Create package category",
               description = "Create a new package category for the logged-in payer")
    public ResponseEntity<PackageCategoryResponse> createPackageCategory(
            @Valid @RequestBody PackageCategoryRequest request) {
        log.info("Creating package category for logged-in payer");
        return packageCategoryService.createPackageCategory(request);
    }

    @PutMapping("/{categoryUuid}")
    @Operation(summary = "Update package category", 
               description = "Update an existing package category")
    public ResponseEntity<PackageCategoryResponse> updatePackageCategory(
            @Parameter(description = "Category UUID") @PathVariable String categoryUuid,
            @Valid @RequestBody PackageCategoryRequest request) {
        log.info("Updating package category: {}", categoryUuid);
        return packageCategoryService.updatePackageCategory(categoryUuid, request);
    }

    @GetMapping("/{categoryUuid}")
    @Operation(summary = "Get package category", 
               description = "Get package category details by UUID")
    public PackageCategoryResponse getPackageCategory(
            @Parameter(description = "Category UUID") @PathVariable String categoryUuid) {
        return packageCategoryService.getPackageCategory(categoryUuid);
    }

    @GetMapping
    @Operation(summary = "Get package categories",
               description = "Get paginated list of package categories for the logged-in payer")
    public PagedResponse<PackageCategoryResponse> getPackageCategories(
            @Parameter(description = "Search keyword") @RequestParam(required = false) String searchKey,
            @Parameter(description = "Status filter (ACTIVE/INACTIVE)") @RequestParam(required = false, defaultValue = "ACTIVE") String status,
            @Parameter(description = "Uuid of the payer") @RequestParam(required = false) String payerUuid,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "25") int size) {
        return packageCategoryService.getPackageCategories(searchKey, status,payerUuid, page, size);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active package categories",
               description = "Get all active package categories for the logged-in payer (for dropdowns)")
    public List<PackageCategoryResponse> getActivePackageCategories() {
        return packageCategoryService.getActivePackageCategories();
    }

    @PatchMapping("/{categoryUuid}/deactivate")
    @Operation(summary = "Deactivate package category", 
               description = "Deactivate a package category")
    public ResponseEntity<String> deactivatePackageCategory(
            @Parameter(description = "Category UUID") @PathVariable String categoryUuid) {
        log.info("Deactivating package category: {}", categoryUuid);
        return packageCategoryService.deactivatePackageCategory(categoryUuid);
    }

    @PatchMapping("/{categoryUuid}/activate")
    @Operation(summary = "Activate package category", 
               description = "Activate a package category")
    public ResponseEntity<String> activatePackageCategory(
            @Parameter(description = "Category UUID") @PathVariable String categoryUuid) {
        log.info("Activating package category: {}", categoryUuid);
        return packageCategoryService.activatePackageCategory(categoryUuid);
    }

    @DeleteMapping("/{categoryUuid}")
    @Operation(summary = "Delete package category", 
               description = "Delete a package category (only if no dependencies exist)")
    public ResponseEntity<String> deletePackageCategory(
            @Parameter(description = "Category UUID") @PathVariable String categoryUuid) {
        log.info("Deleting package category: {}", categoryUuid);
        return packageCategoryService.deletePackageCategory(categoryUuid);
    }

    @GetMapping("/validate-code")
    @Operation(summary = "Validate category code",
               description = "Check if category code is unique for the logged-in payer")
    public ResponseEntity<Boolean> validateCategoryCode(
            @Parameter(description = "Category code to validate") @RequestParam String categoryCode) {
        boolean isUnique = packageCategoryService.isCategoryCodeUnique(categoryCode);
        return ResponseEntity.ok(isUnique);
    }

    @GetMapping("/validate-name")
    @Operation(summary = "Validate category name",
               description = "Check if category name is unique for the logged-in payer")
    public ResponseEntity<Boolean> validateCategoryName(
            @Parameter(description = "Category name to validate") @RequestParam String categoryName) {
        boolean isUnique = packageCategoryService.isCategoryNameUnique(categoryName);
        return ResponseEntity.ok(isUnique);
    }

    //for hc awash
    @GetMapping("/packageInsurance/eligible-categories/{insuredUuid}")
    @Operation(summary = "Get eligible package categories for insured from external insurance",
            description = "Fetches eligible package categories from external system for the given insured")
    public ResponseEntity<List<ExternalPackageCategoryResponse>> getEligiblePackageCategories(
            @Parameter(description = "UUID of the insured")
            @PathVariable String insuredUuid) {

        List<ExternalPackageCategoryResponse> packages =
                packageCategoryService.getEligiblePackages(insuredUuid);

        return ResponseEntity.ok(packages);
    }
}
