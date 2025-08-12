package com.medco.HealthConnectProvider.controller.packageCategory;

import com.medco.HealthConnectProvider.services.packageCategory.PackageCategoryUsageService;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.PackageCategoryUsageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/package-category-usage")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Package Category Usage", description = "APIs for managing package category usage tracking")
@RequiredArgsConstructor
@Slf4j
public class PackageCategoryUsageController {

    private final PackageCategoryUsageService usageService;

    @PostMapping("/record-consumption")
    @Operation(summary = "Record service consumption", 
               description = "Record service consumption from category limits")
    public ResponseEntity<List<PackageCategoryUsageResponse>> recordServiceConsumption(
            @Parameter(description = "Insured person UUID") @RequestParam String insuredUuid,
            @Parameter(description = "Contract detail UUID") @RequestParam String contractDetailUuid,
            @Parameter(description = "Service amount") @RequestParam BigDecimal serviceAmount,
            @Parameter(description = "Quantity") @RequestParam(required = false) Double quantity,
            @Parameter(description = "Service date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime serviceDate,
            @Parameter(description = "Claim UUID") @RequestParam(required = false) String claimUuid,
            @Parameter(description = "Provided service UUID") @RequestParam(required = false) String providedServiceUuid,
            @Parameter(description = "Notes") @RequestParam(required = false) String notes) {
        
        log.info("Recording service consumption for insured: {}, amount: {}", insuredUuid, serviceAmount);
        return usageService.recordServiceConsumption(
                insuredUuid, contractDetailUuid, serviceAmount, quantity, 
                serviceDate, claimUuid, providedServiceUuid, notes);
    }

    @GetMapping("/history/insured/{insuredUuid}/category/{categoryUuid}")
    @Operation(summary = "Get usage history", 
               description = "Get usage history for an insured person and category")
    public PagedResponse<PackageCategoryUsageResponse> getUsageHistory(
            @Parameter(description = "Insured person UUID") @PathVariable String insuredUuid,
            @Parameter(description = "Category UUID") @PathVariable String categoryUuid,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return usageService.getUsageHistory(insuredUuid, categoryUuid, page, size);
    }

    @GetMapping("/claim/{claimUuid}")
    @Operation(summary = "Get usage by claim", 
               description = "Get all usage records for a specific claim")
    public List<PackageCategoryUsageResponse> getUsageByClaim(
            @Parameter(description = "Claim UUID") @PathVariable String claimUuid) {
        return usageService.getUsageByClaim(claimUuid);
    }

    @GetMapping("/provided-service/{providedServiceUuid}")
    @Operation(summary = "Get usage by provided service", 
               description = "Get all usage records for a specific provided service")
    public List<PackageCategoryUsageResponse> getUsageByProvidedService(
            @Parameter(description = "Provided service UUID") @PathVariable String providedServiceUuid) {
        return usageService.getUsageByProvidedService(providedServiceUuid);
    }

    @DeleteMapping("/reverse/claim/{claimUuid}")
    @Operation(summary = "Reverse service consumption", 
               description = "Reverse service consumption for a claim (for claim cancellations)")
    public ResponseEntity<String> reverseServiceConsumption(
            @Parameter(description = "Claim UUID") @PathVariable String claimUuid) {
        log.info("Reversing service consumption for claim: {}", claimUuid);
        return usageService.reverseServiceConsumption(claimUuid);
    }

    @GetMapping("/total/insured/{insuredUuid}")
    @Operation(summary = "Get total usage by insured", 
               description = "Get total usage for an insured person across all categories")
    public List<PackageCategoryUsageResponse> getTotalUsageByInsured(
            @Parameter(description = "Insured person UUID") @PathVariable String insuredUuid) {
        return usageService.getTotalUsageByInsured(insuredUuid);
    }

    @GetMapping("/validate-consumption")
    @Operation(summary = "Validate service consumption", 
               description = "Validate if service consumption is within limits before recording")
    public ResponseEntity<Boolean> validateServiceConsumption(
            @Parameter(description = "Insured person UUID") @RequestParam String insuredUuid,
            @Parameter(description = "Contract detail UUID") @RequestParam String contractDetailUuid,
            @Parameter(description = "Service amount") @RequestParam BigDecimal serviceAmount) {
        boolean isValid = usageService.validateServiceConsumption(insuredUuid, contractDetailUuid, serviceAmount);
        return ResponseEntity.ok(isValid);
    }
}
