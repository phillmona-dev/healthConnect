package com.medco.HealthConnectProvider.services.packageCategory;

import com.medco.HealthConnectProvider.entity.claims.Claim;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ClaimCategoryIntegrationService {

    /**
     * Validate and record category usage when a claim is submitted
     */
    ResponseEntity<String> validateAndRecordClaimUsage(Claim claim);

    /**
     * Reverse category usage when a claim is cancelled or rejected
     */
    ResponseEntity<String> reverseClaimUsage(String claimUuid);

    /**
     * Validate if claim services can be consumed within category limits
     */
    boolean validateClaimAgainstLimits(Claim claim);

    /**
     * Record usage for a specific service in a claim
     */
    ResponseEntity<String> recordServiceUsage(
            String insuredUuid,
            String contractDetailUuid,
            BigDecimal serviceAmount,
            Double quantity,
            LocalDateTime serviceDate,
            String claimUuid,
            String notes);

    /**
     * Check if claim has any category usage records
     */
    boolean hasUsageRecords(String claimUuid);
}
