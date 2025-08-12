package com.medco.HealthConnectProvider.controller.payer;

import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.services.payer.PayerService;
import com.medco.HealthConnectProvider.ui.request.auth.password.payer.PayerRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PayerImportResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PayerProviderResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PayerResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PolicyHolderListResponse;
import com.medco.HealthConnectProvider.ui.response.provider.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/v1/healthConnect/payer")
@Tag(name = "Payer Management", description = "APIs for managing payers/insurance companies")
public class PayerController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    PayerRepository payerRepository;

    @Autowired
    PayerService payerService;

    @Value("${file.upload-dir}")
    private String uploadDirectory;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create payer", description = "Creates a new payer/insurance company with logo")
    public PayerResponse createInstitution(
            @RequestPart("payerRequest") @Valid PayerRequest payerRequest,
            @RequestPart(value = "logo", required = false) MultipartFile logo) {
        return payerService.createPayer(payerRequest, logo);
    }

    @PutMapping(path="/{payerUuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update payer", description = "Updates an existing payer/insurance company with optional logo update")
    public PayerResponse updateInstitution(
            @PathVariable String payerUuid,
            @RequestPart("payerRequest") @Valid PayerRequest payerRequest,
            @RequestPart(value = "logo", required = false) MultipartFile logo) {
        return payerService.updatePayer(payerUuid, payerRequest, logo);
    }

    @PutMapping(path="/updateInstitutionStatus/{payerUuid}")
    @Operation(summary = "Update payer status", description = "Updates the status of a payer")
    public ResponseEntity<?> updateInstitutionStatus(@PathVariable String payerUuid, @RequestParam Status payerStatus) {
        return payerService.updatePayerStatus(payerUuid, payerStatus);
    }

    @GetMapping(path="/{payerUuid}")
    //@PreAuthorize("hasRole('Read-Payer')")
    @Operation(summary = "Get payer", description = "Retrieves a specific payer by UUID")
    public PayerResponse getPayer(@PathVariable String payerUuid) {
        return payerService.getPayer(payerUuid);
    }

    @GetMapping("/list")
    //@PreAuthorize("hasRole('Read-Providers')")
    @Operation(
            summary = "List payers",
            description = "Retrieves a list of Payers with pagination, search, and advanced filtering options"
    )
    public PagedResponse<PayerResponse> getPayers(
            @RequestParam(value = "search", required = false) String searchKey,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit,
            @RequestParam(value = "status", required = false) Status status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "payerName", required = false) String payerName,
            @RequestParam(value = "tinNumber", required = false) Long tinNumber,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc", required = false) String sortDir) {

        return payerService.getPayersWithFilters(searchKey, page, limit, status, category,
                payerName, tinNumber, level, sortBy, sortDir);

    }

    @GetMapping("/list/without_logo")
    @Operation(
            summary = "List payers",
            description = "Retrieves a list of Payers with out logo with pagination, search, and advanced filtering options"
    )
    public ResponseEntity<PagedResponse<PayerResponse>> getPayersWithOutLogo(
            @RequestParam(value = "search", required = false) String searchKey,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit,
            @RequestParam(value = "status", required = false) Status status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "payerName", required = false) String payerName,
            @RequestParam(value = "tinNumber", required = false) Long tinNumber,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc", required = false) String sortDir) {

        PagedResponse<PayerResponse> response = payerService.getPayersWithFiltersWithOutLogo(
                searchKey, page, limit, status, category, payerName, tinNumber, level, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/policy-holders/list")
    //@PreAuthorize("hasRole('Read-Institutions')")
    @Operation(summary = "List policy holders", description = "Retrieves a list of policy holders with pagination, search, and status filtering")
    public List<PolicyHolderListResponse> getPolicyHolders(
            @RequestParam(value="search", defaultValue="", required=false) String search,
            @RequestParam(value="page", defaultValue = "1") int page,
            @RequestParam(value="limit", defaultValue = "25") int limit,
            @RequestParam Status status) {
        return payerService.getPolicyHolders(search, page, limit, status);
    }

    @GetMapping("/provider-assigned/policy-holders/list")
    @Operation(summary = "List provider policy holders", description = "Retrieves policy holders assigned to a specific provider and payer")
    public List<PayerProviderResponse> getProviderPolicyHolders(
            @RequestParam String providerUuid,
            @RequestParam String payerUuid) {
        return payerService.getProviderPolicyHolders(providerUuid, payerUuid);
    }

    @DeleteMapping(path="/{payerUuid}")
    //@PreAuthorize("hasRole('Delete-Payer')")
    @Operation(summary = "Delete payer", description = "Deletes a payer by UUID")
    public ResponseEntity<?> deletePayer(@PathVariable String payerUuid) {
        return payerService.deletePayer(payerUuid);
    }

    @GetMapping("/logo/{payerUuid}")
    @Operation(summary = "Get payer logo", description = "Retrieves the logo image for a payer")
    public ResponseEntity<ByteArrayResource> getPayerLogo(@PathVariable String payerUuid) {
        return payerService.getPayerLogo(payerUuid);
    }


    @GetMapping("/{payerUuid}/providers-with-contract")
    @Operation(summary = "Get providers with contract for a payer")
    public ResponseEntity<PagedResponse<ProviderResponse>> getProvidersWithContract(
            @PathVariable String payerUuid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "providerName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search) {
        return payerService.getProvidersWithContract(payerUuid, page, size, sortBy, sortDir, search);
    }


    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importPayers(@RequestParam("file") MultipartFile file) {

        try {
            PayerImportResponse importedPayers = payerService.importPayersFromExcel(file);
            return ResponseEntity.ok(importedPayers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error importing payers: " + e.getMessage()));
        }
    }

    @GetMapping("/without-active-contract")
    //@PreAuthorize("hasRole('ROLE_PROVIDER')")
    @Operation(summary = "Get payers without active contract", description = "Retrieves a list of payers that do not have an active contract with the currently logged in provider")
    public ResponseEntity<PagedResponse<PayerResponse>> getPayersWithoutActiveContract(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit,
            @RequestParam(value = "sortBy", defaultValue = "payerName") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {

        PagedResponse<PayerResponse> response = payerService.getPayersWithoutActiveContract(page, limit, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

}