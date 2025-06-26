//package com.medco.HealthConnectProvider.controller.service;
//
//import com.medco.HealthConnectProvider.services.service.ProvidedServiceService;
//import com.medco.HealthConnectProvider.ui.request.service.ProvidedServiceRequest;
//import com.medco.HealthConnectProvider.ui.response.service.ProvidedServiceResponse;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/v1/healthConnect/provided-services")
//@SecurityRequirement(name = "bearerAuth")
//@Tag(name = "Provided Services Management", description = "APIs for managing healthcare services provided to patients")
//public class ServiceProvidedController {
//
//    @Autowired
//    private ProvidedServiceService providedServiceService;
//
//    @PostMapping("/claim/{claimUuid}")
//    @Operation(summary = "Add provided service", description = "Adds a provided service to a specific claim")
//    public ResponseEntity<?> addProvidedService(
//            @PathVariable String claimUuid,
//            @Valid @RequestBody ProvidedServiceRequest providedServiceRequest) {
//        return providedServiceService.addProvidedService(claimUuid, providedServiceRequest);
//    }
//
//    @PutMapping("/{providedServiceUuid}")
//    @Operation(summary = "Update provided service", description = "Updates an existing provided service by UUID")
//    public ResponseEntity<?> updateProvidedService(
//            @PathVariable String providedServiceUuid,
//            @Valid @RequestBody ProvidedServiceRequest providedServiceRequest) {
//        return providedServiceService.updateProvidedService(providedServiceUuid, providedServiceRequest);
//    }
//
//    @GetMapping("/{providedServiceUuid}")
//    @Operation(summary = "Get provided service", description = "Retrieves a specific provided service by UUID")
//    public ProvidedServiceResponse getProvidedService(@PathVariable String providedServiceUuid) {
//        return providedServiceService.getProvidedService(providedServiceUuid);
//    }
//
//    @GetMapping("/claim/{claimUuid}")
//    @Operation(summary = "List provided services", description = "Retrieves all provided services for a specific claim")
//    public List<ProvidedServiceResponse> getProvidedServicesByClaim(@PathVariable String claimUuid) {
//        return providedServiceService.getProvidedServicesByClaim(claimUuid);
//    }
//
//    @DeleteMapping("/{providedServiceUuid}")
//    @Operation(summary = "Delete provided service", description = "Deletes a provided service by UUID")
//    public ResponseEntity<?> deleteProvidedService(@PathVariable String providedServiceUuid) {
//        return providedServiceService.deleteProvidedService(providedServiceUuid);
//    }
//
//    @GetMapping("/claim/{claimUuid}/total")
//    @Operation(summary = "Get total price", description = "Calculates the total price of all provided services for a specific claim")
//    public ResponseEntity<Double> getTotalPriceForClaim(@PathVariable String claimUuid) {
//        return ResponseEntity.ok(providedServiceService.calculateTotalPriceForClaim(claimUuid));
//    }
//
//    @GetMapping("/claim/{claimUuid}/count")
//    @Operation(summary = "Get service count", description = "Counts the number of provided services for a specific claim")
//    public ResponseEntity<Long> getServiceCountForClaim(@PathVariable String claimUuid) {
//        return ResponseEntity.ok(providedServiceService.countProvidedServicesForClaim(claimUuid));
//    }
//
//    @PostMapping("/claim/{claimUuid}/batch")
//    @Operation(summary = "Add multiple provided services", description = "Adds multiple provided services to a claim in a single operation")
//    public ResponseEntity<?> addMultipleProvidedServices(
//            @PathVariable String claimUuid,
//            @Valid @RequestBody List<ProvidedServiceRequest> providedServiceRequests) {
//        return providedServiceService.addMultipleProvidedServices(claimUuid, providedServiceRequests);
//    }
//}
