//package com.medco.HealthConnectProvider.controller.integration;
//
//import com.medco.HealthConnectProvider.services.integration.PharmacyIntegrationService;
//import com.medco.HealthConnectProvider.ui.request.integration.MedicationDispensingRequest;
//import com.medco.HealthConnectProvider.ui.response.integration.DispensingResponse;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.security.SecurityRequirement;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/v1/healthConnect/integration/pharmacy")
//@SecurityRequirement(name = "bearerAuth")
//@Tag(name = "Pharmacy Integration", description = "APIs for integrating with external pharmacy systems")
//public class PharmacyIntegrationController {
//
//    @Autowired
//    private PharmacyIntegrationService pharmacyIntegrationService;
//
//    @PostMapping("/dispensing")
//    @Operation(summary = "Record medication dispensing",
//               description = "Records medications dispensed to a patient from an external pharmacy system")
//    @PreAuthorize("hasRole('Pharmacy-Integration')")
//    public ResponseEntity<DispensingResponse> recordMedicationDispensing(
//            @Valid @RequestBody MedicationDispensingRequest request) {
//        return pharmacyIntegrationService.recordMedicationDispensing(request);
//    }
//
//    @GetMapping("/dispensing/{providerUuid}")
//    @Operation(summary = "Get pending dispensing records",
//               description = "Retrieves dispensing records that haven't been included in a claim yet")
//    @PreAuthorize("hasRole('View-Dispensing-Records')")
//    public ResponseEntity<?> getPendingDispensingRecords(
//            @PathVariable String providerUuid,
//            @RequestParam(required = false) String patientId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//        return pharmacyIntegrationService.getPendingDispensingRecords(providerUuid, patientId, page, size);
//    }
//
//    @PostMapping("/dispensing/batch-claim/{providerUuid}")
//    @Operation(summary = "Create claim from dispensing records",
//               description = "Creates a new claim from selected dispensing records")
//    @PreAuthorize("hasRole('Create-Pharmacy-Claim')")
//    public ResponseEntity<?> createClaimFromDispensingRecords(
//            @PathVariable String providerUuid,
//            @RequestBody String[] dispensingUuids) {
//        return pharmacyIntegrationService.createClaimFromDispensingRecords(providerUuid, dispensingUuids);
//    }
//}