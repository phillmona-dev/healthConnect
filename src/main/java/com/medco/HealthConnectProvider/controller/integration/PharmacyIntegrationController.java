package com.medco.HealthConnectProvider.controller.integration;

import com.medco.HealthConnectProvider.dto.PendingDispensingRecordDTO;
import com.medco.HealthConnectProvider.services.eligibility.EligibilityService;
import com.medco.HealthConnectProvider.services.integration.PharmacyIntegrationService;
import com.medco.HealthConnectProvider.ui.request.eligibility.EligibilityCheckRequest;
import com.medco.HealthConnectProvider.ui.request.integration.DispensingRecordRequest;
import com.medco.HealthConnectProvider.ui.request.integration.MedicationDispensingRequest;
import com.medco.HealthConnectProvider.ui.response.integration.DispensingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/healthConnect/integration/pharmacy")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pharmacy Integration", description = "APIs for integrating with external pharmacy systems")
public class PharmacyIntegrationController {

    @Autowired
    private PharmacyIntegrationService pharmacyIntegrationService;

    @Autowired
    private EligibilityService eligibilityService;

    @PostMapping("/check")
    @Operation(summary = "Check patient eligibility", description = "Verifies if a patient is eligible for services based on their identification and insurance")
    public ResponseEntity<?> checkEligibility(
            @RequestParam String providerUuid,
            @Valid @RequestBody EligibilityCheckRequest request) {
        return eligibilityService.checkEligibility(providerUuid, request);
    }

    @PostMapping("/dispensing")
    @Operation(summary = "Record medication dispensing",
               description = "Records medications dispensed to a patient from an external pharmacy system")
    //@PreAuthorize("hasRole('Pharmacy-Integration')")
    public ResponseEntity<DispensingResponse> recordMedicationDispensing(
            @Valid @RequestBody MedicationDispensingRequest request) {
        return pharmacyIntegrationService.recordMedicationDispensing(request);
    }

    @GetMapping("/dispensing/{providerUuid}")
    @Operation(summary = "Get dispensing records with advanced search",
            description = "Retrieves dispensing records with various filter options")
    public ResponseEntity<Page<PendingDispensingRecordDTO>> getDispensingRecords(
            @PathVariable String providerUuid,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String medicationName,
            @RequestParam(required = false) String patientName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dispensingDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        return pharmacyIntegrationService.getDispensingRecords(providerUuid, phone, status, startDate, endDate,
                medicationName, patientName, page, size, sortBy, sortDirection);
    }

    @PostMapping("/dispensing/batch-claim/{providerUuid}")
    @Operation(summary = "Create claim from dispensing records",
               description = "Creates a new claim from selected dispensing records")
    //@PreAuthorize("hasRole('Create-Pharmacy-Claim')")
    public ResponseEntity<?> createClaimFromDispensingRecords(
            @PathVariable String providerUuid,
            @RequestBody String[] dispensingUuids) {
        return pharmacyIntegrationService.createClaimFromDispensingRecords(providerUuid, dispensingUuids);
    }

    @PostMapping("/dispensing/authorize/{dispensingUuid}")
    @Operation(summary = "Authorize single dispensing record",
            description = "Changes the status of a single dispensing record from PENDING to AUTHORIZED")
    public ResponseEntity<?> authorizeDispensingRecord(@PathVariable String dispensingUuid) {
        return pharmacyIntegrationService.authorizeDispensingRecord(dispensingUuid);
    }

    @PostMapping("/dispensing/authorize-batch")
    @Operation(summary = "Authorize multiple dispensing records",
            description = "Changes the status of selected dispensing records from PENDING to AUTHORIZED")
    public ResponseEntity<?> authorizeDispensingRecords(@RequestBody String[] dispensingUuids) {
        return pharmacyIntegrationService.authorizeDispensingRecords(dispensingUuids);
    }

    @PostMapping("/dispensing/create-claim/{providerUuid}/{dispensingUuid}")
    @Operation(summary = "Create claim from single authorized dispensing record",
            description = "Creates a new claim from a single authorized dispensing record and marks it as SUBMITTED")
    public ResponseEntity<?> createClaimFromAuthorizedRecord(
            @PathVariable String providerUuid,
            @PathVariable String dispensingUuid) {
        return pharmacyIntegrationService.createClaimFromAuthorizedRecord(providerUuid, dispensingUuid);
    }

    @PostMapping("/dispensing/create-claim-batch/{providerUuid}")
    @Operation(summary = "Create claim from multiple authorized dispensing records",
            description = "Creates a new claim from authorized dispensing records and marks them as SUBMITTED")
    public ResponseEntity<?> createClaimFromAuthorizedRecords(
            @PathVariable String providerUuid,
            @RequestBody String[] dispensingUuids) {
        return pharmacyIntegrationService.createClaimFromAuthorizedRecords(providerUuid, dispensingUuids);
    }


    @PutMapping("/dispensing/update-status/{providerUuid}")
    @Operation(summary = "Update status of dispensing records",
            description = "Changes the status of one or more dispensing records to either AUTHORIZED or SUBMITTED")
    public ResponseEntity<?> updateDispensingRecordsStatus(
            @PathVariable String providerUuid,
            @RequestParam String newStatus,
            @RequestBody String[] dispensingUuids) {
        return pharmacyIntegrationService.updateDispensingRecordsStatus(providerUuid, newStatus, dispensingUuids);
    }

    @PostMapping(value = "/dispensing-records", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a new dispensing record", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> addDispensingRecord(@RequestBody DispensingRecordRequest request) {
        return pharmacyIntegrationService.addDispensingRecord(request);
    }
}