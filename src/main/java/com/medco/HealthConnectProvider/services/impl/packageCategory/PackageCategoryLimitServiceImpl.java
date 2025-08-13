package com.medco.HealthConnectProvider.services.impl.packageCategory;

import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategory;
import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategoryLimit;
import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategoryUsage;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.PackageCategoryLimitRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.PackageCategoryRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.PackageCategoryUsageRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.ServiceCategoryMappingRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.services.packageCategory.PackageCategoryLimitService;
import com.medco.HealthConnectProvider.ui.request.packageCategory.PackageCategoryLimitRequest;
import com.medco.HealthConnectProvider.ui.response.packageCategory.CategoryLimitSummaryResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.PackageCategoryLimitResponse;
import com.medco.HealthConnectProvider.utils.enums.LimitType;
import com.medco.HealthConnectProvider.utils.enums.PeriodType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
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
public class PackageCategoryLimitServiceImpl implements PackageCategoryLimitService {

    private final PackageCategoryLimitRepository limitRepository;
    private final PackageCategoryRepository categoryRepository;
    private final ContractRepository contractRepository;
    private final PackageCategoryUsageRepository usageRepository;
    private final ServiceCategoryMappingRepository mappingRepository;
    private final InsuredRepository insuredRepository;

    @Override
    public ResponseEntity<List<PackageCategoryLimitResponse>> setCategoryLimits(String contractUuid, 
                                                                               List<PackageCategoryLimitRequest> requests) {
        log.info("Setting category limits for contract: {}", contractUuid);

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null){
            throw new ResourceNotFoundException("Contract not found with UUID: " + contractUuid);
        }

        List<PackageCategoryLimit> limits = new ArrayList<>();

        for (PackageCategoryLimitRequest request : requests) {
            PackageCategory category = categoryRepository.findByCategoryUuid(request.getCategoryUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with UUID: " + request.getCategoryUuid()));

            boolean limitExists = limitRepository.existsByPackageCategoryAndContractHeaderAndIsActiveTrueAndResetDateAfter(
                    category, contract, LocalDate.now());

            if (limitExists) {
                throw new BadRequestException("Active limit already exists for category: " + category.getCategoryName());
            }

            PackageCategoryLimit limit = PackageCategoryLimit.builder()
                    .packageCategory(category)
                    .contractHeader(contract)
                    .limitType(LimitType.valueOf(request.getLimitType()))
                    .limitValue(request.getLimitValue())
                    .periodType(PeriodType.valueOf(request.getPeriodType()))
                    .resetDate(request.getResetDate())
                    .description(request.getDescription())
                    .isActive(true)
                    .build();

            limits.add(limitRepository.save(limit));
        }

        List<PackageCategoryLimitResponse> responses = limits.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        log.info("Successfully set {} category limits for contract: {}", limits.size(), contractUuid);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @Override
    public ResponseEntity<PackageCategoryLimitResponse> updateCategoryLimit(String limitUuid, 
                                                                           PackageCategoryLimitRequest request) {
        log.info("Updating category limit: {}", limitUuid);

        PackageCategoryLimit limit = limitRepository.findByLimitUuid(limitUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Category limit not found with UUID: " + limitUuid));

        limit.setLimitType(LimitType.valueOf(request.getLimitType()));
        limit.setLimitValue(request.getLimitValue());
        limit.setPeriodType(PeriodType.valueOf(request.getPeriodType()));
        limit.setResetDate(request.getResetDate());
        limit.setDescription(request.getDescription());

        PackageCategoryLimit updatedLimit = limitRepository.save(limit);
        log.info("Category limit updated successfully: {}", limitUuid);

        return ResponseEntity.ok(mapToResponse(updatedLimit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageCategoryLimitResponse> getCategoryLimitsByContract(String contractUuid) {
        List<PackageCategoryLimit> limits = limitRepository.findActiveByContractUuid(contractUuid);
        return limits.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PackageCategoryLimitResponse getCategoryLimit(String limitUuid) {
        PackageCategoryLimit limit = limitRepository.findByLimitUuid(limitUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Category limit not found with UUID: " + limitUuid));

        return mapToResponse(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryLimitSummaryResponse getCategoryLimitSummary(String insuredUuid, String contractUuid) {
        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null){
            throw new ResourceNotFoundException("Insured person not found with UUID: " + insuredUuid);
        }

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null){
            throw new ResourceNotFoundException("Contract not found with UUID: " + contractUuid);
        }


        List<PackageCategoryLimit> limits = limitRepository.findByContractHeaderAndIsActiveTrueOrderByPackageCategory_CategoryNameAsc(contract);

        List<CategoryLimitSummaryResponse.CategoryLimitDetail> limitDetails = limits.stream()
                .map(limit -> {
                    LocalDate periodStart = calculatePeriodStart(limit.getPeriodType(), limit.getResetDate());
                    LocalDate periodEnd = limit.getResetDate();

                    BigDecimal usedAmount = usageRepository.getTotalUsedAmountByInsuredAndCategoryAndPeriod(
                            insured, limit, periodStart, periodEnd);
                    Double usedQuantity = usageRepository.getTotalUsedQuantityByInsuredAndCategoryAndPeriod(
                            insured, limit, periodStart, periodEnd);
                    Integer usedVisits = usageRepository.getTotalUsedVisitsByInsuredAndCategoryAndPeriod(
                            insured, limit, periodStart, periodEnd);

                    BigDecimal remainingAmount = limit.getLimitValue().subtract(usedAmount);
                    double utilizationPercentage = usedAmount.divide(limit.getLimitValue(), 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();

                    return CategoryLimitSummaryResponse.CategoryLimitDetail.builder()
                            .categoryUuid(limit.getPackageCategory().getCategoryUuid())
                            .categoryName(limit.getPackageCategory().getCategoryName())
                            .categoryCode(limit.getPackageCategory().getCategoryCode())
                            .limitType(limit.getLimitType().toString())
                            .limitValue(limit.getLimitValue())
                            .usedAmount(usedAmount)
                            .remainingAmount(remainingAmount)
                            .usedQuantity(usedQuantity)
                            .usedVisits(usedVisits)
                            .periodType(limit.getPeriodType().toString())
                            .resetDate(limit.getResetDate())
                            .isExpired(limit.isExpired())
                            .utilizationPercentage(utilizationPercentage)
                            .build();
                })
                .collect(Collectors.toList());

        return CategoryLimitSummaryResponse.builder()
                .insuredPersonUuid(insured.getInsuredUuid())
                .insuredPersonName(insured.getFirstName() + " " + insured.getFatherName())
                .contractUuid(contract.getContractHeaderUuid())
                .contractName(contract.getContractName())
                .categoryLimits(limitDetails)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canConsumeService(String insuredUuid, String contractDetailUuid, BigDecimal serviceAmount) {
        List<String> categoryUuids = mappingRepository.findActiveByContractDetailUuid(contractDetailUuid)
                .stream()
                .map(mapping -> mapping.getPackageCategory().getCategoryUuid())
                .collect(Collectors.toList());

        if (categoryUuids.isEmpty()) {
            return true;
        }

        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null){
            throw new ResourceNotFoundException("Insured person not found with UUID: " + insuredUuid);
        }

        for (String categoryUuid : categoryUuids) {
            BigDecimal remainingLimit = getRemainingLimitInternal(insured, categoryUuid);
            if (remainingLimit.compareTo(serviceAmount) < 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getRemainingLimit(String insuredUuid, String categoryUuid, String contractUuid) {
        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null){
            throw new ResourceNotFoundException("Insured person not found with UUID: " + insuredUuid);
        }

        return getRemainingLimitInternal(insured, categoryUuid);
    }

    @Override
    public ResponseEntity<String> deactivateCategoryLimit(String limitUuid) {
        log.info("Deactivating category limit: {}", limitUuid);

        PackageCategoryLimit limit = limitRepository.findByLimitUuid(limitUuid)
                .orElseThrow(() -> new RuntimeException("Category limit not found with UUID: " + limitUuid));

        limit.setActive(false);
        limitRepository.save(limit);

        log.info("Category limit deactivated successfully: {}", limitUuid);
        return ResponseEntity.ok("Category limit deactivated successfully");
    }

    @Override
    @Scheduled(cron = "0 0 1 * * ?") // Run daily at 1 AM
    public void resetExpiredLimits() {
        log.info("Starting scheduled reset of expired limits");

        List<PackageCategoryLimit> expiredLimits = limitRepository.findExpiredLimits(LocalDate.now());
        
        for (PackageCategoryLimit limit : expiredLimits) {
            LocalDate newResetDate = calculateNextResetDate(limit.getPeriodType(), limit.getResetDate());
            
            PackageCategoryLimit newLimit = PackageCategoryLimit.builder()
                    .packageCategory(limit.getPackageCategory())
                    .contractHeader(limit.getContractHeader())
                    .limitType(limit.getLimitType())
                    .limitValue(limit.getLimitValue())
                    .periodType(limit.getPeriodType())
                    .resetDate(newResetDate)
                    .description(limit.getDescription())
                    .isActive(true)
                    .build();

            limitRepository.save(newLimit);
            
            limit.setActive(false);
            limitRepository.save(limit);
        }

        log.info("Reset {} expired limits", expiredLimits.size());
    }

    private BigDecimal getRemainingLimitInternal(Insured insured, String categoryUuid) {
        // Implementation for getting remaining limit
        // This would involve finding the active limit and calculating usage
        return BigDecimal.ZERO; // Placeholder
    }

    private LocalDate calculatePeriodStart(PeriodType periodType, LocalDate resetDate) {
        return switch (periodType) {
            case ANNUAL -> resetDate.minusYears(1);
            case MONTHLY -> resetDate.minusMonths(1);
            case CONTRACT_PERIOD -> resetDate.minusYears(1); // Assuming 1 year contract
            default -> resetDate.minusMonths(1);
        };
    }

    private LocalDate calculateNextResetDate(PeriodType periodType, LocalDate currentResetDate) {
        return switch (periodType) {
            case ANNUAL -> currentResetDate.plusYears(1);
            case MONTHLY -> currentResetDate.plusMonths(1);
            case CONTRACT_PERIOD -> currentResetDate.plusYears(1);
            default -> currentResetDate.plusMonths(1);
        };
    }

    private PackageCategoryLimitResponse mapToResponse(PackageCategoryLimit limit) {
        BigDecimal totalUsed = limit.getUsageRecords().stream()
                .filter(usage -> !usage.isDeleted())
                .map(PackageCategoryUsage::getUsedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PackageCategoryLimitResponse.builder()
                .limitUuid(limit.getLimitUuid())
                .categoryUuid(limit.getPackageCategory().getCategoryUuid())
                .categoryName(limit.getPackageCategory().getCategoryName())
                .categoryCode(limit.getPackageCategory().getCategoryCode())
                .contractUuid(limit.getContractHeader().getContractHeaderUuid())
                .contractName(limit.getContractHeader().getContractName())
                .limitType(limit.getLimitType().toString())
                .limitValue(limit.getLimitValue())
                .periodType(limit.getPeriodType().toString())
                .resetDate(limit.getResetDate())
                .description(limit.getDescription())
                .isActive(limit.isActive())
                .isExpired(limit.isExpired())
                .totalUsed(totalUsed)
                .remainingLimit(limit.getRemainingLimit())
                .createdAt(limit.getCreatedAt())
                .updatedAt(limit.getUpdatedAt())
                .build();
    }
}
