package com.medco.HealthConnectProvider.controller.packageCategory;

import com.medco.HealthConnectProvider.services.packageCategory.ServiceCategoryMappingService;
import com.medco.HealthConnectProvider.ui.request.packageCategory.ServiceCategoryMappingRequest;
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
}
