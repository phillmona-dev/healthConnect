package com.medco.HealthConnectProvider.services.packageCategory;

import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.PackageCategoryUsageResponse;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PackageCategoryUsageService {

    /**
     * Record service consumption from category limits
     */
    ResponseEntity<List<PackageCategoryUsageResponse>> recordServiceConsumption(
            String insuredUuid, 
            String contractDetailUuid, 
            BigDecimal serviceAmount,
            Double quantity,
            LocalDateTime serviceDate,
            String claimUuid,
            String providedServiceUuid,
            String notes);

    /**
     * Get usage history for an insured person and category
     */
    PagedResponse<PackageCategoryUsageResponse> getUsageHistory(String insuredUuid, String categoryUuid, 
                                                               int page, int size);

    /**
     * Get usage by claim UUID
     */
    List<PackageCategoryUsageResponse> getUsageByClaim(String claimUuid);

    /**
     * Get usage by provided service UUID
     */
    List<PackageCategoryUsageResponse> getUsageByProvidedService(String providedServiceUuid);

    /**
     * Reverse service consumption (for claim cancellations)
     */
    ResponseEntity<String> reverseServiceConsumption(String claimUuid);

    /**
     * Get total usage for an insured person across all categories
     */
    List<PackageCategoryUsageResponse> getTotalUsageByInsured(String insuredUuid);

    /**
     * Validate if service consumption is within limits before recording
     */
    boolean validateServiceConsumption(String insuredUuid, String contractDetailUuid, BigDecimal serviceAmount);
}
