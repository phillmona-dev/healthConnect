package com.medco.HealthConnectProvider.controller.integration;

import com.medco.HealthConnectProvider.dto.BatchRecordDTO;
import com.medco.HealthConnectProvider.dto.MedicationDispensingDTO;
import com.medco.HealthConnectProvider.dto.PendingDispensingRecordDTO;
import com.medco.HealthConnectProvider.services.claims.BatchRecordService;
import com.medco.HealthConnectProvider.services.eligibility.EligibilityService;
import com.medco.HealthConnectProvider.services.integration.PharmacyIntegrationService;
import com.medco.HealthConnectProvider.ui.request.drug.DrugDispensingRecordRequest;
import com.medco.HealthConnectProvider.ui.request.integration.DispensingRecordRequest;
import com.medco.HealthConnectProvider.ui.request.integration.KenemaPharmacyDispensingRequest;
import com.medco.HealthConnectProvider.ui.response.claims.ReconciliationResponse;
import com.medco.HealthConnectProvider.ui.response.integration.DispensingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/integration/pharmacy")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pharmacy Integration", description = "APIs for integrating with external pharmacy systems")
public class PharmacyIntegrationController {

    private static final Logger logger = LoggerFactory.getLogger(PharmacyIntegrationController.class);


    private final PharmacyIntegrationService pharmacyIntegrationService;

    private final EligibilityService eligibilityService;

    private final BatchRecordService batchRecordService;


    public PharmacyIntegrationController(PharmacyIntegrationService pharmacyIntegrationService, EligibilityService eligibilityService, BatchRecordService batchRecordService) {
        this.pharmacyIntegrationService = pharmacyIntegrationService;
        this.eligibilityService = eligibilityService;
        this.batchRecordService = batchRecordService;
    }

    @GetMapping("/check")
    @Operation(summary = "check patient eligibility",
            description = "verifies if a patient is eligible based on their identifiers")
    public ResponseEntity<?> checkEligibility(
            @RequestParam String identifier
    ){
     return eligibilityService.checkEligibility(identifier);
    }

    @PostMapping("/dispensing")
    @Operation(summary = "Record medication dispensing",
            description = "Records medications dispensed to a patient from Kenema pharmacies")
    public ResponseEntity<DispensingResponse> recordMedicationDispensing(
            @Valid @RequestBody KenemaPharmacyDispensingRequest request) {
        logger.info("Received request: {}", request);
        return pharmacyIntegrationService.recordMedicationDispensing(request);
    }

    @GetMapping("/dispensing/{providerUuid}")
    @Operation(summary = "Get dispensing records with advanced search",
            description = "Retrieves dispensing records with various filter options")
    public ResponseEntity<Page<PendingDispensingRecordDTO>> getDispensingRecords(
            @PathVariable String providerUuid,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String payerUuid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "dispensingDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        return pharmacyIntegrationService.getDispensingRecords(providerUuid, search, status, startDate, endDate,
                payerUuid, page, size, sortBy, sortDirection);
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

//TODO THIS API AUTHORIZES A SINGLE DISPENSING MEDICATION
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
            description = "Changes the status of one or more dispensing records to either SUBMITTED or AUTHORIZED")
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

    @PutMapping("/reconcile/{claimUuid}")
    @Operation(
            summary = "Reconcile payment for a claim by the provider",
            description = "Allows the provider (pharmacy) to reconcile the payment received for a specific claim identified by its UUID. " +
                    "This process updates the claim status to RECONCILED and creates a batch record for financial tracking."
    )
    public ResponseEntity<ReconciliationResponse> reconcilePayment(
            @Parameter(description = "UUID of the claim to be reconciled", required = true)
            @PathVariable String claimUuid
    ) {
        return pharmacyIntegrationService.reconcilePayment(claimUuid);
    }

    @GetMapping("/batch")
    @Operation(
            summary = "Search and retrieve batch records",
            description = "Retrieves a paginated list of batch records based on the provided search criteria, with options for sorting and pagination."
    )
    public ResponseEntity<Page<BatchRecordDTO>> searchBatchRecords(
            @Parameter(description = "Search term for batch code, payer name, total amount, status, or claim UUID")
            @RequestParam(required = false) String search,

            @Parameter(description = "Start date for requested on range")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime requestedOnStart,

            @Parameter(description = "End date for requested on range")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime requestedOnEnd,

            @Parameter(description = "Start date for claim dating range")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate claimDatingFrom,

            @Parameter(description = "End date for claim dating range")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate claimDatingTo,

            @Parameter(description = "Page number (1-based)")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Number of records per page")
            @RequestParam(defaultValue = "25") int size,

            @Parameter(description = "Field to sort by (e.g., 'requestedOn', 'batchCode', 'totalAmount')")
            @RequestParam(defaultValue = "requestedOn") String sortBy,

            @Parameter(description = "Sort direction ('asc' for ascending, 'desc' for descending)")
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {

        Page<BatchRecordDTO> results = batchRecordService.searchBatchRecords(
                search, requestedOnStart, requestedOnEnd, claimDatingFrom, claimDatingTo,
                page - 1, size, sortBy, sortDirection
        );
        return ResponseEntity.ok(results);
    }

    @PostMapping(value = "/drug-dispensing-records", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a new drug dispensing record", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> addDrugDispensingRecord(@RequestBody DrugDispensingRecordRequest request) {
        return pharmacyIntegrationService.addDrugDispensingRecord(request);
    }


    @GetMapping("/medications/{batchCode}")
    @Operation(summary = "Get medications by batch code",
            description = "Retrieves a list of medications associated with a specific batch code")
    public ResponseEntity<List<MedicationDispensingDTO>> getMedicationsByBatchCode(@PathVariable String batchCode) {
        return pharmacyIntegrationService.getMedicationsByBatchCode(batchCode);
    }


}