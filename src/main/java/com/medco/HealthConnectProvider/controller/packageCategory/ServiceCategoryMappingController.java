package com.medco.HealthConnectProvider.controller.packageCategory;

import com.medco.HealthConnectProvider.services.packageCategory.ServiceCategoryMappingService;
import com.medco.HealthConnectProvider.ui.request.packageCategory.BulkServiceCategoryAssignmentRequest;
import com.medco.HealthConnectProvider.ui.request.packageCategory.EligibleServiceSearchRequest;
import com.medco.HealthConnectProvider.ui.request.packageCategory.ServiceCategoryMappingRequest;
import com.medco.HealthConnectProvider.ui.response.packageCategory.BulkServiceCategoryAssignmentResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.EligibleServiceResponse;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.ExternalPackageEligibleServicesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/service-category-mappings")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Service Category Mappings", description = "APIs for managing service to category mappings")
@RequiredArgsConstructor
@Slf4j
public class ServiceCategoryMappingController {

    private final ServiceCategoryMappingService mappingService;

    @PostMapping
    @Operation(summary = "Map service to categories", 
               description = "Map a service to one or more package categories")
    public ResponseEntity<String> mapServiceToCategories(
            @Valid @RequestBody ServiceCategoryMappingRequest request) {
        log.info("Mapping service {} to categories", request.getContractDetailUuid());
        return mappingService.mapServiceToCategories(request);
    }

    @DeleteMapping("/service/{contractDetailUuid}/category/{categoryUuid}")
    @Operation(summary = "Remove service from category", 
               description = "Remove a service from a specific category")
    public ResponseEntity<String> removeServiceFromCategory(
            @Parameter(description = "Contract detail UUID") @PathVariable String contractDetailUuid,
            @Parameter(description = "Category UUID") @PathVariable String categoryUuid) {
        log.info("Removing service {} from category {}", contractDetailUuid, categoryUuid);
        return mappingService.removeServiceFromCategory(contractDetailUuid, categoryUuid);
    }

    @GetMapping("/service/{contractDetailUuid}/categories")
    @Operation(summary = "Get categories by service", 
               description = "Get all categories mapped to a specific service")
    public List<String> getCategoriesByService(
            @Parameter(description = "Contract detail UUID") @PathVariable String contractDetailUuid) {
        return mappingService.getCategoriesByService(contractDetailUuid);
    }

    @GetMapping("/category/{categoryUuid}/services")
    @Operation(summary = "Get services by category", 
               description = "Get all services mapped to a specific category")
    public List<String> getServicesByCategory(
            @Parameter(description = "Category UUID") @PathVariable String categoryUuid) {
        return mappingService.getServicesByCategory(categoryUuid);
    }

    @PutMapping("/{mappingUuid}")
    @Operation(summary = "Update service category mapping",
               description = "Update an existing service category mapping")
    public ResponseEntity<String> updateServiceCategoryMapping(
            @Parameter(description = "Mapping UUID") @PathVariable String mappingUuid,
            @Valid @RequestBody ServiceCategoryMappingRequest request) {
        log.info("Updating service category mapping: {}", mappingUuid);
        return mappingService.updateServiceCategoryMapping(mappingUuid, request);
    }

    @PostMapping("/bulk-assign")
    @Operation(summary = "Assign multiple services to category",
               description = "Assign multiple contract details (eligible services) to a package category")
    public ResponseEntity<BulkServiceCategoryAssignmentResponse> assignServicesToCategory(
            @Valid @RequestBody BulkServiceCategoryAssignmentRequest request) {
        log.info("Bulk assigning {} services to category {}",
                request.getContractDetailUuids().size(), request.getCategoryUuid());
        return mappingService.assignServicesToCategory(request);
    }

    @GetMapping("/eligible-services")
    @Operation(summary = "Get eligible services for category",
            description = "Fetch contract details (eligible services) for a selected category and contract. " +
                    "The searchKey parameter searches across service name, code, description, category name, and contract name.")
    public PagedResponse<EligibleServiceResponse> getEligibleServicesForCategory(
            @Parameter(description = "Contract UUID") @RequestParam String contractUuid,
            @Parameter(description = "Category name") @RequestParam String categoryName,

            @Parameter(description = "Search key (searches service name, code, description, category name, contract name)")
            @RequestParam(required = false) String searchKey,

            @Parameter(description = "Service code (exact match)") @RequestParam(required = false) String serviceCode,
            @Parameter(description = "Service category") @RequestParam(required = false) String serviceCategory,
            @Parameter(description = "Service sub-category") @RequestParam(required = false) String serviceSubCategory,
            @Parameter(description = "List of service codes (comma separated)") @RequestParam(required = false) List<String> serviceCodes,
            @Parameter(description = "List of service codes to exclude (comma separated)") @RequestParam(required = false) List<String> excludeServiceCodes,

            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Price type (SERVICE_PRICE, CONTRACT_PRICE)") @RequestParam(required = false) String priceType,

            @Parameter(description = "Service status") @RequestParam(required = false) String status,
            @Parameter(description = "Consumes from limit") @RequestParam(required = false) Boolean consumesFromLimit,

            @Parameter(description = "Date range (TODAY, WEEK, MONTH, YEAR)") @RequestParam(required = false) String dateRange,

            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "25") int size,
            @Parameter(description = "Sort field") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (ASC, DESC)") @RequestParam(required = false) String sortDirection) {

        EligibleServiceSearchRequest search = EligibleServiceSearchRequest.builder()
                .page(page)
                .size(size)
                .contractUuid(contractUuid)
                .categoryName(categoryName)
                .searchKey(searchKey)
                .serviceCode(serviceCode)
                .serviceCategory(serviceCategory)
                .serviceSubCategory(serviceSubCategory)
                .serviceCodes(serviceCodes)
                .excludeServiceCodes(excludeServiceCodes)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .priceType(priceType)
                .status(status)
                .consumesFromLimit(consumesFromLimit)
                .dateRange(dateRange)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        log.info("Fetching eligible services for category: {} in contract: {} with search key: {}",
                categoryName, contractUuid, searchKey);

        return mappingService.getEligibleServicesForCategory(search);
    }

    @GetMapping("/packageInsurance/eligible-services")
    @Operation(summary = "Get eligible services for a package",
            description = "Fetches eligible services from external system for the given package and insured")
    public ResponseEntity<?> getEligibleServices(
            @Parameter(description = "Contract UUID", required = true)
            @RequestParam @NotBlank String contractUuid,

            @Parameter(description = "Package UUID", required = true)
            @RequestParam @NotBlank String packageUuid,

            @Parameter(description = "Insured UUID", required = true)
            @RequestParam @NotBlank String insuredUuid,

            @Parameter(description = "Search term (optional)")
            @RequestParam(required = false) String search,

            @Parameter(description = "Page number (1-based, optional)")
            @RequestParam(required = false, defaultValue = "1") Integer page,

            @Parameter(description = "Items per page (optional)")
            @RequestParam(required = false, defaultValue = "25") Integer limit) {

        ExternalPackageEligibleServicesResponse response = mappingService.getEligibleServices(
                contractUuid,
                packageUuid,
                insuredUuid,
                search,
                page,
                limit);

        // If response indicates no data, return simple message
        if ("NO_DATA".equals(response.getStatus())) {
            String message = response.getPackageName() != null
                ? response.getPackageName()
                : "No data available";
            return ResponseEntity.ok(message);
        }

        return ResponseEntity.ok(response);
    }

}
