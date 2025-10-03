package com.medco.HealthConnectProvider.controller.integration;

import com.medco.HealthConnectProvider.annotation.RequiresApiKey;
import com.medco.HealthConnectProvider.dto.BatchRecordDTO;
import com.medco.HealthConnectProvider.dto.MedicationDispensingDTO;
import com.medco.HealthConnectProvider.dto.PendingDispensingRecordDTO;
import com.medco.HealthConnectProvider.services.claims.BatchRecordService;
import com.medco.HealthConnectProvider.services.eligibility.EligibilityService;
import com.medco.HealthConnectProvider.services.integration.PharmacyIntegrationService;
import com.medco.HealthConnectProvider.ui.request.drug.DrugDispensingRecordEditRequest;
import com.medco.HealthConnectProvider.ui.request.drug.DrugDispensingRecordRequest;
import com.medco.HealthConnectProvider.ui.request.integration.DispensingRecordEditRequest;
import com.medco.HealthConnectProvider.ui.request.integration.CreateCbhiInsuredRequest;
import com.medco.HealthConnectProvider.ui.request.integration.CreateBulkCbhiInsuredRequest;
import com.medco.HealthConnectProvider.ui.request.integration.DispensingRecordRequest;
import com.medco.HealthConnectProvider.ui.request.integration.KenemaPharmacyDispensingRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.claims.ReconciliationResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PayerResponse;
import com.medco.HealthConnectProvider.ui.response.integration.BulkCbhiInsuredResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.ui.response.integration.DispensingDetailResponse;
import com.medco.HealthConnectProvider.ui.response.integration.DispensingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Enumeration;

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

    @RequiresApiKey
    @GetMapping("/check")
    @Operation(summary = "check patient eligibility",
            description = "verifies if a patient is eligible based on their identifiers")
    public ResponseEntity<?> checkEligibility(
            @RequestParam String identifier
    ){
     return eligibilityService.checkEligibility(identifier);
    }

    @RequiresApiKey
    @PostMapping("/dispensing")
    @Operation(summary = "Record medication dispensing",
            description = "Records medications dispensed to a patient from Kenema pharmacies")
    public ResponseEntity<DispensingResponse> recordMedicationDispensing(
            @Valid @RequestBody KenemaPharmacyDispensingRequest request,
            HttpServletRequest httpRequest) {

        // Log all request headers
        logger.info("========== Incoming Request Details ==========");
        logger.info("Request URL: {}", httpRequest.getRequestURL());
        logger.info("Request Method: {}", httpRequest.getMethod());
        logger.info("Remote Address: {}", httpRequest.getRemoteAddr());
        logger.info("Remote Host: {}", httpRequest.getRemoteHost());
        logger.info("Content Type: {}", httpRequest.getContentType());
        logger.info("Content Length: {}", httpRequest.getContentLength());

        logger.info("========== Request Headers ==========");
        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = httpRequest.getHeader(headerName);
            logger.info("Header: {} = {}", headerName, headerValue);
        }

        logger.info("========== Request Body ==========");
        logger.info("Request Body: {}", request);
        logger.info("===========================================");

        return pharmacyIntegrationService.recordMedicationDispensing(request);
    }

    @GetMapping("/dispensing/{providerUuid}")
    @Operation(summary = "Get dispensing records with advanced search",
            description = "Retrieves dispensing records with various filter options")
    public ResponseEntity<PagedResponse<PendingDispensingRecordDTO>> getDispensingRecords(
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

    @PutMapping("/claim/update-status/{medicationDispensingUuid}")
    @Operation(summary = "Update claim status of dispensing records",
            description = "Changes the status a service or a drug claim")
    public ResponseEntity<?> updateDispensingRecordsStatus(
            @PathVariable String medicationDispensingUuid,
            @RequestParam String newStatus,
            @RequestParam(value = "remark",required = false)String remark) {
        return pharmacyIntegrationService.updateServiceClaimStatus( medicationDispensingUuid,newStatus,remark);
    }

    @PostMapping(value = "/dispensing-records", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a new dispensing record", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> addDispensingRecord(@RequestPart("request") DispensingRecordRequest request,
                                                 @RequestPart(value = "attachment", required = false) MultipartFile attachment) {
        return pharmacyIntegrationService.addDispensingRecord(request, attachment);
    }

    @PostMapping(value = "/dispensing-records", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a new dispensing record (JSON)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> addDispensingRecord(@RequestBody DispensingRecordRequest request) {
        return pharmacyIntegrationService.addDispensingRecord(request, null);
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

            @RequestParam(required = false) String status,

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

        Page<BatchRecordDTO> results = batchRecordService.searchBatchRecords(status,
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
            description = "Retrieves a paginated list of medications associated with a specific batch code")
    public ResponseEntity<PagedResponse<MedicationDispensingDTO>> getMedicationsByBatchCode(
            @PathVariable String batchCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return pharmacyIntegrationService.getMedicationsByBatchCode(batchCode, page, size);
    }

    @GetMapping("/getDispensingDetail/{dispensingUuid}")
    @Operation(summary = "Get dispensing detail",
            description = "Retrieves detailed information about a specific dispensing record")
    public ResponseEntity<DispensingDetailResponse> getDispensingDetail(
            @PathVariable String dispensingUuid) {
        return pharmacyIntegrationService.getDispensingDetail(dispensingUuid);
    }

    @PutMapping("/updateDispensing/{dispensingUuid}")
    @Operation(summary = "Edit a dispensing record",
            description = "Edits an existing dispensing record identified by its UUID. " +
                    "For REJECTED records, you can optionally change status to RESUBMITTED by including claimStatus=RESUBMITTED in the request.")
    public ResponseEntity<?> editDispensingRecord(
            @PathVariable String dispensingUuid,
            @Valid @RequestBody DispensingRecordEditRequest editRequest) {
        return pharmacyIntegrationService.editDispensingRecord(dispensingUuid, editRequest);
    }

    @PutMapping("/drug-dispensing/{dispensingUuid}")
    @Operation(summary = "Edit drug dispensing record",
            description = "Edits an existing drug dispensing record identified by its UUID")
    public ResponseEntity<?> editDrugDispensingRecord(
            @PathVariable String dispensingUuid,
            @Valid @RequestBody DrugDispensingRecordEditRequest editRequest) {
        logger.info("Received request to edit drug dispensing record: {}", dispensingUuid);
        return pharmacyIntegrationService.editDrugDispensingRecord(dispensingUuid, editRequest);
    }

    @PutMapping("/dispensing/{dispensingUuid}/remove-from-batch")
    @Operation(
            summary = "Remove dispensing record from batch",
            description = "Removes a medication dispensing record from its associated batch and changes its status to DRAFT. " +
                    "This operation is typically used when a dispensing record needs to be edited or reviewed further " +
                    "before being included in a batch for claim processing."
    )
    public ResponseEntity<?> removeDispensingFromBatch(
            @Parameter(description = "UUID of the dispensing record to be removed from batch", required = true)
            @PathVariable String dispensingUuid
    ) {
        return pharmacyIntegrationService.removeDispensingFromBatch(dispensingUuid);
    }

    @RequiresApiKey
    @PostMapping("/insured")
    @Operation(summary = "Create insured member",
            description = "Creates a new insured member for integration with Kenema pharmacy management system")
    public ResponseEntity<?> createCbhiInsured(@Valid @RequestBody CreateCbhiInsuredRequest request) {
        logger.info("Received request to create insured: {}", request);
        return pharmacyIntegrationService.createCbhiInsured(request);
    }

//  @RequiresApiKey
    @PostMapping("/insured/bulk")
    @Operation(summary = "Create multiple insured members",
            description = "Creates multiple insured members under a single payer for integration with Kenema pharmacy management system")
    public ResponseEntity<BulkCbhiInsuredResponse> createBulkCbhiInsured(@Valid @RequestBody CreateBulkCbhiInsuredRequest request) {
        logger.info("Received request to create bulk insured for payer: {}, count: {}",
                request.getPayerUuid(), request.getInsuredMembers().size());
        return pharmacyIntegrationService.createBulkCbhiInsured(request);
    }

    @RequiresApiKey
    @GetMapping("/payers")
    @Operation(summary = "Search payers for integration",
            description = "Retrieves a paginated list of payers with advanced search and filters for external system integration")
    public ResponseEntity<PagedResponse<PayerResponse>> getPayersForIntegration(
            @RequestParam(value = "search", required = false) String searchKey,
            @RequestParam(value = "status", required = false) Status status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "payerName", required = false) String payerName,
            @RequestParam(value = "tinNumber", required = false) Long tinNumber,
            @RequestParam(value = "isInsurance", required = false) Boolean isInsurance,
            @RequestParam(value = "isCbhi", required = false) Boolean isCbhi,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "sortBy", defaultValue = "payerName") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "asc") String sortDirection
    ) {
        return pharmacyIntegrationService.getPayersForIntegration(
                searchKey, status, category, payerName, tinNumber, isInsurance, isCbhi, page, size, sortBy, sortDirection
        );
    }

}