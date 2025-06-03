package com.medco.HealthConnectProvider.controller.payer;

import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.services.payer.PayerService;
import com.medco.HealthConnectProvider.ui.request.auth.password.payer.PayerRequest;
import com.medco.HealthConnectProvider.ui.request.search.PayerSearchRequest;
import com.medco.HealthConnectProvider.ui.response.payer.PayerProviderResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PayerResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PolicyHolderListResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

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


    @PutMapping(path="/{institutionUuid}")
    //@PreAuthorize("hasRole('Update-Institution')")
    @Operation(summary = "Update payer", description = "Updates an existing payer/insurance company")
    public PayerResponse updateInstitution(@PathVariable String institutionUuid, @Valid @RequestBody PayerRequest institutionRequest) {
        return payerService.updatePayer(institutionUuid, institutionRequest);
    }

    @PutMapping(path="/set-institution-insurance-number/{institutionUuid}")
    @Operation(summary = "Set payer insurance number", description = "Sets the insurance number for a payer")
    public ResponseEntity<?> setInstitutionInsuranceNumber(@PathVariable String payerUuid, @Valid @RequestParam String payerInsuranceNumber) {
        return payerService.setPayerInsuranceNumber(payerUuid, payerInsuranceNumber);
    }

    @PutMapping(path="/updateInstitutionStatus/{institutionUuid}")
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
    public List<PayerResponse> getPayers(
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

}