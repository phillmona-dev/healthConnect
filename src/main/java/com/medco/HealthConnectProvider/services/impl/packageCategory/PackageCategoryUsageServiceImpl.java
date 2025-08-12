package com.medco.HealthConnectProvider.services.impl.packageCategory;

import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategoryLimit;
import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategoryUsage;
import com.medco.HealthConnectProvider.entity.packageCategory.ServiceCategoryMapping;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.contract.ContractDetailRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.PackageCategoryLimitRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.PackageCategoryUsageRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.ServiceCategoryMappingRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.services.packageCategory.PackageCategoryUsageService;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.PackageCategoryUsageResponse;
import com.medco.HealthConnectProvider.utils.enums.PeriodType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PackageCategoryUsageServiceImpl implements PackageCategoryUsageService {

    private final PackageCategoryUsageRepository usageRepository;
    private final PackageCategoryLimitRepository limitRepository;
    private final ServiceCategoryMappingRepository mappingRepository;
    private final ContractDetailRepository contractDetailRepository;
    private final InsuredRepository insuredRepository;

    @Override
    public ResponseEntity<List<PackageCategoryUsageResponse>> recordServiceConsumption(
            String insuredUuid, 
            String contractDetailUuid, 
            BigDecimal serviceAmount,
            Double quantity,
            LocalDateTime serviceDate,
            String claimUuid,
            String providedServiceUuid,
            String notes) {

        log.info("Recording service consumption for insured: {}, service amount: {}", insuredUuid, serviceAmount);

        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null){
            throw new ResourceNotFoundException("Insured person not found with UUID: " + insuredUuid);
        }

        ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(contractDetailUuid);
        if (contractDetail == null){
            throw new ResourceNotFoundException("Contract detail not found with UUID: " + contractDetailUuid);
        }

        // Get all category mappings for this service
        List<ServiceCategoryMapping> mappings = mappingRepository.findActiveByContractDetailUuid(contractDetailUuid);

        if (mappings.isEmpty()) {
            log.info("No category mappings found for service: {}", contractDetailUuid);
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<PackageCategoryUsage> usageRecords = new ArrayList<>();

        for (ServiceCategoryMapping mapping : mappings) {
            if (!mapping.isConsumesFromLimit()) {
                continue; // Skip if this mapping doesn't consume from limit
            }

            // Find active limit for this category and contract
            PackageCategoryLimit activeLimit = limitRepository.findActiveLimitByCategoryAndContract(
                    mapping.getPackageCategory().getCategoryUuid(),
                    contractDetail.getContractHeaderUuid(),
                    LocalDate.now()
            ).orElse(null);

            if (activeLimit == null) {
                log.warn("No active limit found for category: {} in contract: {}", 
                        mapping.getPackageCategory().getCategoryName(), 
                        contractDetail.getContractHeaderUuid());
                continue;
            }

            // Check if consumption would exceed limit
            BigDecimal currentUsage = getCurrentUsageForPeriod(insured, activeLimit);
            BigDecimal newTotal = currentUsage.add(serviceAmount);

            if (newTotal.compareTo(activeLimit.getLimitValue()) > 0) {
                throw new BadRequestException(
                    String.format("Service consumption would exceed limit for category '%s'. " +
                                "Limit: %s, Current usage: %s, Requested: %s", 
                                mapping.getPackageCategory().getCategoryName(),
                                activeLimit.getLimitValue(),
                                currentUsage,
                                serviceAmount));
            }

            // Calculate period dates
            LocalDate periodStart = calculatePeriodStart(activeLimit.getPeriodType(), activeLimit.getResetDate());
            LocalDate periodEnd = activeLimit.getResetDate();

            // Create usage record
            PackageCategoryUsage usage = PackageCategoryUsage.builder()
                    .insuredPerson(insured)
                    .categoryLimit(activeLimit)
                    .periodStartDate(periodStart)
                    .periodEndDate(periodEnd)
                    .usedAmount(serviceAmount)
                    .usedQuantity(quantity != null ? quantity : 1.0)
                    .usedVisits(1)
                    .serviceDate(serviceDate)
                    .serviceUuid(contractDetail.getServiceUuid())
                    .serviceName(contractDetail.getServicelist().getServiceName())
                    .claimUuid(claimUuid)
                    .providedServiceUuid(providedServiceUuid)
                    .notes(notes)
                    .build();

            usageRecords.add(usageRepository.save(usage));
        }

        List<PackageCategoryUsageResponse> responses = usageRecords.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        log.info("Successfully recorded {} usage records for insured: {}", usageRecords.size(), insuredUuid);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PackageCategoryUsageResponse> getUsageHistory(String insuredUuid, String categoryUuid, 
                                                                      int page, int size) {
        // Implementation would require a custom query to join insured and category
        // For now, returning empty response
        return new PagedResponse<>(new ArrayList<>(), page, size, 0L, 0, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageCategoryUsageResponse> getUsageByClaim(String claimUuid) {
        List<PackageCategoryUsage> usageRecords = usageRepository.findByClaimUuid(claimUuid);
        return usageRecords.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageCategoryUsageResponse> getUsageByProvidedService(String providedServiceUuid) {
        List<PackageCategoryUsage> usageRecords = usageRepository.findByProvidedServiceUuid(providedServiceUuid);
        return usageRecords.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<String> reverseServiceConsumption(String claimUuid) {
        log.info("Reversing service consumption for claim: {}", claimUuid);

        List<PackageCategoryUsage> usageRecords = usageRepository.findByClaimUuid(claimUuid);

        if (usageRecords.isEmpty()) {
            throw new ResourceNotFoundException("No usage records found for claim: " + claimUuid);
        }

        // Mark usage records as deleted (soft delete)
        for (PackageCategoryUsage usage : usageRecords) {
            usage.setDeleted(true);
            usageRepository.save(usage);
        }

        log.info("Successfully reversed {} usage records for claim: {}", usageRecords.size(), claimUuid);
        return ResponseEntity.ok("Service consumption reversed successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageCategoryUsageResponse> getTotalUsageByInsured(String insuredUuid) {
        List<PackageCategoryUsage> usageRecords = usageRepository.findByInsuredUuidAndCategoryUuid(insuredUuid, null);
        return usageRecords.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateServiceConsumption(String insuredUuid, String contractDetailUuid, BigDecimal serviceAmount) {
        try {
            Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
            if (insured == null){
                throw new ResourceNotFoundException("Insured person not found with UUID: " + insuredUuid);
            }


            ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(contractDetailUuid);
            if (contractDetail == null){
                throw new ResourceNotFoundException("Contract detail not found with UUID: " + contractDetailUuid);
            }

            List<ServiceCategoryMapping> mappings = mappingRepository.findActiveByContractDetailUuid(contractDetailUuid);

            for (ServiceCategoryMapping mapping : mappings) {
                if (!mapping.isConsumesFromLimit()) {
                    continue;
                }

                PackageCategoryLimit activeLimit = limitRepository.findActiveLimitByCategoryAndContract(
                        mapping.getPackageCategory().getCategoryUuid(),
                        contractDetail.getContractHeaderUuid(),
                        LocalDate.now()
                ).orElse(null);

                if (activeLimit == null) {
                    continue;
                }

                BigDecimal currentUsage = getCurrentUsageForPeriod(insured, activeLimit);
                BigDecimal newTotal = currentUsage.add(serviceAmount);

                if (newTotal.compareTo(activeLimit.getLimitValue()) > 0) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Error validating service consumption: {}", e.getMessage());
            return false;
        }
    }

    private BigDecimal getCurrentUsageForPeriod(Insured insured, PackageCategoryLimit limit) {
        LocalDate periodStart = calculatePeriodStart(limit.getPeriodType(), limit.getResetDate());
        LocalDate periodEnd = limit.getResetDate();

        return usageRepository.getTotalUsedAmountByInsuredAndCategoryAndPeriod(
                insured, limit, periodStart, periodEnd);
    }

    private LocalDate calculatePeriodStart(PeriodType periodType, LocalDate resetDate) {
        switch (periodType) {
            case ANNUAL:
                return resetDate.minusYears(1);
            case MONTHLY:
                return resetDate.minusMonths(1);
            case CONTRACT_PERIOD:
                return resetDate.minusYears(1); // Assuming 1 year contract
            default:
                return resetDate.minusMonths(1);
        }
    }

    private PackageCategoryUsageResponse mapToResponse(PackageCategoryUsage usage) {
        return PackageCategoryUsageResponse.builder()
                .usageUuid(usage.getUsageUuid())
                .insuredPersonUuid(usage.getInsuredPerson().getInsuredUuid())
                .insuredPersonName(usage.getInsuredPerson().getFirstName() + " " + usage.getInsuredPerson().getFatherName())
                .categoryUuid(usage.getCategoryLimit().getPackageCategory().getCategoryUuid())
                .categoryName(usage.getCategoryLimit().getPackageCategory().getCategoryName())
                .categoryCode(usage.getCategoryLimit().getPackageCategory().getCategoryCode())
                .periodStartDate(usage.getPeriodStartDate())
                .periodEndDate(usage.getPeriodEndDate())
                .usedAmount(usage.getUsedAmount())
                .usedQuantity(usage.getUsedQuantity())
                .usedVisits(usage.getUsedVisits())
                .serviceDate(usage.getServiceDate())
                .serviceUuid(usage.getServiceUuid())
                .serviceName(usage.getServiceName())
                .claimUuid(usage.getClaimUuid())
                .providedServiceUuid(usage.getProvidedServiceUuid())
                .notes(usage.getNotes())
                .remainingAmount(usage.getRemainingAmount())
                .createdAt(LocalDateTime.from(usage.getCreatedAt()))
                .build();
    }
}
