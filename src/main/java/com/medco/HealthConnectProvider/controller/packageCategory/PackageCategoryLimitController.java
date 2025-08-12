package com.medco.HealthConnectProvider.controller.packageCategory;

import com.medco.HealthConnectProvider.services.packageCategory.PackageCategoryLimitService;
import com.medco.HealthConnectProvider.ui.request.packageCategory.PackageCategoryLimitRequest;
import com.medco.HealthConnectProvider.ui.response.packageCategory.CategoryLimitSummaryResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.PackageCategoryLimitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/package-category-limits")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Package Category Limits", description = "APIs for managing package category limits")
@RequiredArgsConstructor
@Slf4j
public class PackageCategoryLimitController {

    private final PackageCategoryLimitService limitService;

    @PostMapping("/contract/{contractUuid}")
    @Operation(summary = "Set category limits for contract", 
               description = "Set category limits for a specific contract")
    public ResponseEntity<List<PackageCategoryLimitResponse>> setCategoryLimits(
            @Parameter(description = "Contract UUID") @PathVariable String contractUuid,
            @Valid @RequestBody List<PackageCategoryLimitRequest> requests) {
        log.info("Setting category limits for contract: {}", contractUuid);
        return limitService.setCategoryLimits(contractUuid, requests);
    }

    @PutMapping("/{limitUuid}")
    @Operation(summary = "Update category limit", 
               description = "Update an existing category limit")
    public ResponseEntity<PackageCategoryLimitResponse> updateCategoryLimit(
            @Parameter(description = "Limit UUID") @PathVariable String limitUuid,
            @Valid @RequestBody PackageCategoryLimitRequest request) {
        log.info("Updating category limit: {}", limitUuid);
        return limitService.updateCategoryLimit(limitUuid, request);
    }

    @GetMapping("/contract/{contractUuid}")
    @Operation(summary = "Get category limits by contract", 
               description = "Get all category limits for a specific contract")
    public List<PackageCategoryLimitResponse> getCategoryLimitsByContract(
            @Parameter(description = "Contract UUID") @PathVariable String contractUuid) {
        return limitService.getCategoryLimitsByContract(contractUuid);
    }

    @GetMapping("/{limitUuid}")
    @Operation(summary = "Get category limit", 
               description = "Get category limit details by UUID")
    public PackageCategoryLimitResponse getCategoryLimit(
            @Parameter(description = "Limit UUID") @PathVariable String limitUuid) {
        return limitService.getCategoryLimit(limitUuid);
    }

    @GetMapping("/summary/insured/{insuredUuid}/contract/{contractUuid}")
    @Operation(summary = "Get category limit summary", 
               description = "Get category limit summary for an insured person in a specific contract")
    public CategoryLimitSummaryResponse getCategoryLimitSummary(
            @Parameter(description = "Insured person UUID") @PathVariable String insuredUuid,
            @Parameter(description = "Contract UUID") @PathVariable String contractUuid) {
        return limitService.getCategoryLimitSummary(insuredUuid, contractUuid);
    }

    @GetMapping("/validate-consumption")
    @Operation(summary = "Validate service consumption", 
               description = "Check if a service can be consumed within category limits")
    public ResponseEntity<Boolean> canConsumeService(
            @Parameter(description = "Insured person UUID") @RequestParam String insuredUuid,
            @Parameter(description = "Contract detail UUID") @RequestParam String contractDetailUuid,
            @Parameter(description = "Service amount") @RequestParam BigDecimal serviceAmount) {
        boolean canConsume = limitService.canConsumeService(insuredUuid, contractDetailUuid, serviceAmount);
        return ResponseEntity.ok(canConsume);
    }

    @GetMapping("/remaining-limit")
    @Operation(summary = "Get remaining limit", 
               description = "Get remaining limit for a specific category and insured person")
    public ResponseEntity<BigDecimal> getRemainingLimit(
            @Parameter(description = "Insured person UUID") @RequestParam String insuredUuid,
            @Parameter(description = "Category UUID") @RequestParam String categoryUuid,
            @Parameter(description = "Contract UUID") @RequestParam String contractUuid) {
        BigDecimal remainingLimit = limitService.getRemainingLimit(insuredUuid, categoryUuid, contractUuid);
        return ResponseEntity.ok(remainingLimit);
    }

    @PatchMapping("/{limitUuid}/deactivate")
    @Operation(summary = "Deactivate category limit", 
               description = "Deactivate a category limit")
    public ResponseEntity<String> deactivateCategoryLimit(
            @Parameter(description = "Limit UUID") @PathVariable String limitUuid) {
        log.info("Deactivating category limit: {}", limitUuid);
        return limitService.deactivateCategoryLimit(limitUuid);
    }

    @PostMapping("/reset-expired")
    @Operation(summary = "Reset expired limits", 
               description = "Manually trigger reset of expired limits (normally runs automatically)")
    public ResponseEntity<String> resetExpiredLimits() {
        log.info("Manually triggering reset of expired limits");
        limitService.resetExpiredLimits();
        return ResponseEntity.ok("Expired limits reset successfully");
    }
}
