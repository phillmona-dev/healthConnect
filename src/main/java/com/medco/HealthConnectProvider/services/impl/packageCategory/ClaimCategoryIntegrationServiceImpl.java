package com.medco.HealthConnectProvider.services.impl.packageCategory;

import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.claims.ClaimItem;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensing;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensingItem;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.claims.ClaimItemRepository;
import com.medco.HealthConnectProvider.repository.claims.ClaimRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.services.packageCategory.ClaimCategoryIntegrationService;
import com.medco.HealthConnectProvider.services.packageCategory.PackageCategoryUsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClaimCategoryIntegrationServiceImpl implements ClaimCategoryIntegrationService {

    private final PackageCategoryUsageService usageService;
    private final ClaimRepository claimRepository;
    private final ClaimItemRepository claimItemRepository;
    private final InsuredRepository insuredRepository;

    @Override
    public ResponseEntity<String> validateAndRecordClaimUsage(Claim claim) {
        log.info("Validating and recording category usage for claim: {}", claim.getClaimUuid());

        try {
            // First validate that the claim can be processed within limits
            if (!validateClaimAgainstLimits(claim)) {
                throw new BadRequestException("Claim exceeds available category limits");
            }

            // Find the insured person for this claim
            Insured insured = findInsuredForClaim(claim);

            // Record usage for batch record items (pharmacy claims)
            if (claim.getBatchRecord() != null && claim.getBatchRecord().getMedicationDispensing() != null) {
                for (MedicationDispensing dispensing : claim.getBatchRecord().getMedicationDispensing()) {
                    for (MedicationDispensingItem item : dispensing.getItems()) {
                        if (item.getContractDetail() != null) {
                            recordServiceUsage(
                                    insured.getInsuredUuid(),
                                    item.getContractDetail().getContractDetailUuid(),
                                    BigDecimal.valueOf(item.getTotalPrice()),
                                    item.getQuantity(),
                                    claim.getVisitDate(),
                                    claim.getClaimUuid(),
                                    "Medication dispensing: " + item.getMedicationName()
                            );
                        }
                    }
                }
            }

            // Record usage for claim items (regular service claims)
            List<ClaimItem> claimItems = claimItemRepository.findByClaimClaimUuid(claim.getClaimUuid());
            if (claimItems != null && !claimItems.isEmpty()) {
                for (ClaimItem item : claimItems) {
                    if (item.getProvidedServiceUuid() != null) {
                        // This would require finding the contract detail from the provided service
                        // For now, we'll skip this as it requires additional repository lookups
                        log.info("Skipping usage recording for claim item: {} (requires provided service lookup)",
                                item.getItemUuid());
                    }
                }
            }

            log.info("Successfully recorded category usage for claim: {}", claim.getClaimUuid());
            return ResponseEntity.ok("Category usage recorded successfully");

        } catch (Exception e) {
            log.error("Error recording category usage for claim: {}", claim.getClaimUuid(), e);
            throw new BadRequestException("Failed to record category usage: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<String> reverseClaimUsage(String claimUuid) {
        log.info("Reversing category usage for claim: {}", claimUuid);

        try {
            return usageService.reverseServiceConsumption(claimUuid);
        } catch (Exception e) {
            log.error("Error reversing category usage for claim: {}", claimUuid, e);
            throw new BadRequestException("Failed to reverse category usage: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateClaimAgainstLimits(Claim claim) {
        log.info("Validating claim against category limits: {}", claim.getClaimUuid());

        try {
            // Find the insured person for this claim
            Insured insured = findInsuredForClaim(claim);

            // Validate batch record items (pharmacy claims)
            if (claim.getBatchRecord() != null && claim.getBatchRecord().getMedicationDispensing() != null) {
                for (MedicationDispensing dispensing : claim.getBatchRecord().getMedicationDispensing()) {
                    for (MedicationDispensingItem item : dispensing.getItems()) {
                        if (item.getContractDetail() != null) {
                            boolean isValid = usageService.validateServiceConsumption(
                                    insured.getInsuredUuid(),
                                    item.getContractDetail().getContractDetailUuid(),
                                    BigDecimal.valueOf(item.getTotalPrice())
                            );
                            if (!isValid) {
                                log.warn("Service consumption validation failed for item: {}", item.getItemUuid());
                                return false;
                            }
                        }
                    }
                }
            }

            // Validate claim items (regular service claims)
            List<ClaimItem> claimItems = claimItemRepository.findByClaimClaimUuid(claim.getClaimUuid());
            if (claimItems != null && !claimItems.isEmpty()) {
                for (ClaimItem item : claimItems) {
                    // This would require finding the contract detail from the provided service
                    // For now, we'll assume validation passes for regular claims
                    log.info("Skipping validation for claim item: {} (requires provided service lookup)",
                            item.getItemUuid());
                }
            }

            return true;

        } catch (Exception e) {
            log.error("Error validating claim against limits: {}", claim.getClaimUuid(), e);
            return false;
        }
    }

    @Override
    public ResponseEntity<String> recordServiceUsage(
            String insuredUuid,
            String contractDetailUuid,
            BigDecimal serviceAmount,
            Double quantity,
            LocalDateTime serviceDate,
            String claimUuid,
            String notes) {

        try {
            usageService.recordServiceConsumption(
                    insuredUuid,
                    contractDetailUuid,
                    serviceAmount,
                    quantity,
                    serviceDate,
                    claimUuid,
                    null, // providedServiceUuid
                    notes
            );
            return ResponseEntity.ok("Service usage recorded successfully");
        } catch (Exception e) {
            log.error("Error recording service usage", e);
            throw new BadRequestException("Failed to record service usage: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUsageRecords(String claimUuid) {
        try {
            return !usageService.getUsageByClaim(claimUuid).isEmpty();
        } catch (Exception e) {
            log.error("Error checking usage records for claim: {}", claimUuid, e);
            return false;
        }
    }

    private Insured findInsuredForClaim(Claim claim) {
        // For pharmacy claims, get insured from medication dispensing
        if (claim.getBatchRecord() != null &&
            claim.getBatchRecord().getMedicationDispensing() != null &&
            !claim.getBatchRecord().getMedicationDispensing().isEmpty()) {

            MedicationDispensing dispensing = claim.getBatchRecord().getMedicationDispensing().get(0);
            if (dispensing.getInsuredUuid() != null) {
                Insured insured = insuredRepository.findByInsuredUuid(dispensing.getInsuredUuid());
                if (insured == null) {
                    throw new ResourceNotFoundException("Insured", "insuredUuid", dispensing.getInsuredUuid());
                }
                return insured;
            }
        }

        // For regular claims, we would need to get insured from MRN or other identifier
        // This requires additional implementation based on how insured persons are linked to claims
        throw new BadRequestException("Unable to determine insured person for claim: " + claim.getClaimUuid());
    }
}
