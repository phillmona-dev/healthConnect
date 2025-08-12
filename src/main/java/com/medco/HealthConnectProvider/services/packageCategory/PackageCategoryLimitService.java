package com.medco.HealthConnectProvider.services.packageCategory;

import com.medco.HealthConnectProvider.ui.request.packageCategory.PackageCategoryLimitRequest;
import com.medco.HealthConnectProvider.ui.response.packageCategory.CategoryLimitSummaryResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.PackageCategoryLimitResponse;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

public interface PackageCategoryLimitService {

    /**
     * Set category limits for a contract
     */
    ResponseEntity<List<PackageCategoryLimitResponse>> setCategoryLimits(String contractUuid, 
                                                                        List<PackageCategoryLimitRequest> requests);

    /**
     * Update a specific category limit
     */
    ResponseEntity<PackageCategoryLimitResponse> updateCategoryLimit(String limitUuid, 
                                                                    PackageCategoryLimitRequest request);

    /**
     * Get all category limits for a contract
     */
    List<PackageCategoryLimitResponse> getCategoryLimitsByContract(String contractUuid);

    /**
     * Get category limit by UUID
     */
    PackageCategoryLimitResponse getCategoryLimit(String limitUuid);

    /**
     * Get category limit summary for an insured person
     */
    CategoryLimitSummaryResponse getCategoryLimitSummary(String insuredUuid, String contractUuid);

    /**
     * Check if a service can be consumed within category limits
     */
    boolean canConsumeService(String insuredUuid, String contractDetailUuid, BigDecimal serviceAmount);

    /**
     * Get remaining limit for a specific category and insured person
     */
    BigDecimal getRemainingLimit(String insuredUuid, String categoryUuid, String contractUuid);

    /**
     * Deactivate a category limit
     */
    ResponseEntity<String> deactivateCategoryLimit(String limitUuid);

    /**
     * Reset expired limits (scheduled task)
     */
    void resetExpiredLimits();
}
