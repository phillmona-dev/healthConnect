package com.medco.HealthConnectProvider.controller.packageCategory;

import com.medco.HealthConnectProvider.services.packageCategory.ServiceCategoryMappingService;
import com.medco.HealthConnectProvider.ui.request.packageCategory.BulkServiceCategoryAssignmentRequest;
import com.medco.HealthConnectProvider.ui.request.packageCategory.EligibleServiceSearchRequest;
import com.medco.HealthConnectProvider.ui.request.packageCategory.ServiceCategoryMappingRequest;
import com.medco.HealthConnectProvider.ui.response.packageCategory.BulkServiceCategoryAssignmentResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.EligibleServiceResponse;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
               description = "Fetch contract details (eligible services) for a selected category and contract with optional search")
    public PagedResponse<EligibleServiceResponse> getEligibleServicesForCategory(
            @Parameter(description = "Search request containing all search criteria") EligibleServiceSearchRequest search) {

        log.info("Fetching eligible services for category: {} in contract: {} with search key: {}",
                search.getCategoryName(), search.getContractUuid(), search.getSearchKey());

        return mappingService.getEligibleServicesForCategory(search);

    }

}
