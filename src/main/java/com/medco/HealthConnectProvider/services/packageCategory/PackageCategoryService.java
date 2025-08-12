package com.medco.HealthConnectProvider.services.packageCategory;

import com.medco.HealthConnectProvider.ui.request.packageCategory.PackageCategoryRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.PackageCategoryResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface PackageCategoryService {

    /**
     * Create a new package category for a payer (from logged in user context)
     */
    ResponseEntity<PackageCategoryResponse> createPackageCategory(PackageCategoryRequest request);

    /**
     * Update an existing package category
     */
    ResponseEntity<PackageCategoryResponse> updatePackageCategory(String categoryUuid, PackageCategoryRequest request);

    /**
     * Get package category by UUID
     */
    PackageCategoryResponse getPackageCategory(String categoryUuid);

    /**
     * Get all package categories for a payer with pagination and search (from logged in user context)
     */
    PagedResponse<PackageCategoryResponse> getPackageCategories(String searchKey, String status, int page, int size);

    /**
     * Get all active package categories for a payer (for dropdowns, from logged in user context)
     */
    List<PackageCategoryResponse> getActivePackageCategories();

    /**
     * Deactivate a package category (soft delete)
     */
    ResponseEntity<String> deactivatePackageCategory(String categoryUuid);

    /**
     * Activate a package category
     */
    ResponseEntity<String> activatePackageCategory(String categoryUuid);

    /**
     * Delete a package category (hard delete - only if no dependencies)
     */
    ResponseEntity<String> deletePackageCategory(String categoryUuid);

    /**
     * Check if category code is unique for a payer (from logged in user context)
     */
    boolean isCategoryCodeUnique(String categoryCode);

    /**
     * Check if category name is unique for a payer (from logged in user context)
     */
    boolean isCategoryNameUnique(String categoryName);
}
