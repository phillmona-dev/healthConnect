package com.medco.HealthConnectProvider.controller.contract;

import com.medco.HealthConnectProvider.services.contract.ContractService;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.group.ContractServiceGroupAssignmentRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.group.EmployeeGroupRequest;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractListPayerResponse;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.paginationUtils.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/payer-provider-contract")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Contract Management", description = "APIs for managing payer-provider contracts")
public class ContractController {

    @Autowired
    ContractService contractService;

    @PostMapping
    @Operation(summary = "Create a new contract", description = "Creates a new contract between a payer and provider")
    //@PreAuthorize("hasRole('Create-Provider-Contract')")
    public ResponseEntity<?> createContract(@Valid @RequestBody ContractRequest contractRequest) {
        return contractService.createContract(contractRequest);
    }

    @PutMapping(path="/{contractUuid}")
    @Operation(summary = "Update contract", description = "Updates an existing contract by UUID")
    //@PreAuthorize("hasRole('Update-Provider-Contract')")
    public ResponseEntity<?> updateContract(@PathVariable String contractUuid, @Valid @RequestBody ContractRequest contractRequest) {
        return contractService.updateContract(contractUuid, contractRequest);
    }

    @GetMapping(path="/{contractUuid}")
    @Operation(summary = "Get contract details", description = "Retrieves contract details by UUID")
    //@PreAuthorize("hasRole('Read-Provider-Contract')")
    public ContractResponse getContract(@PathVariable String contractUuid) {
        return contractService.getContract(contractUuid);
    }

    @PutMapping(path="/approve/{payerProviderContractUuid}")
    @Operation(summary = "Approve contract", description = "Approves a contract by UUID")
    // @PreAuthorize("hasRole('Approve-Provider-Contract')")
    @PreAuthorize("hasRole('Read-Provider-Contract')")
    public ResponseEntity<?> approveContract(@PathVariable String payerProviderContractUuid) {
        return contractService.approveContract(payerProviderContractUuid);
    }

    @PutMapping(path="/agreed/{payerProviderContractUuid}")
    @Operation(summary = "Provider agreement response", description = "Updates a contract with provider agreement status and remarks")
    // @PreAuthorize("hasRole('Approve-Provider-Contract')")
    @PreAuthorize("hasRole('Read-Provider-Contract')")
    public ResponseEntity<?> providerAgreed(@PathVariable String payerProviderContractUuid,
                                            @RequestParam String status,
                                            @RequestParam String remark) {
        return contractService.payerAgreementResponse(payerProviderContractUuid,status,remark);
    }

    @DeleteMapping(path="/{contractUuid}")
    @Operation(summary = "Delete contract", description = "Deletes a contract by UUID")
//	  @PreAuthorize("hasRole('Delete-Provider-Contracts')")
    public ResponseEntity<?> deleteContract(@PathVariable String contractUuid) {
        return contractService.deleteContract(contractUuid);
    }

    @GetMapping("/provider/lists")
    @Operation(summary = "Get payer-provider contracts", description = "Retrieves a list of payer-provider contracts with pagination and filtering")
//	@PreAuthorize("hasRole('Read-Provider-Contracts')")
    public List<ContractListPayerResponse> getPayerProvidersContractLists(@RequestParam(name = "search", required = false)  String searchKey,
                                                                          @RequestParam(value="page", defaultValue = "1") int page,
                                                                          @RequestParam(value="limit", defaultValue = "25") int limit,
                                                                          @RequestParam String status) {
        Pageable pageable = PaginationUtils.paginateResource(page,limit,"id","desc");
        return contractService.getPayerProvidersContractLists(searchKey,pageable, status);
    }

    @GetMapping("/provider/contract/lists")
    @Operation(summary = "Get provider contracts", description = "Retrieves a list of contracts for a specific provider with pagination and filtering")
//	@PreAuthorize("hasRole('Read-Provider-Contracts')")
    public List<ContractListPayerResponse> getProvidersContractLists(@RequestParam  String providerUuid, @RequestParam(name = "search", required = false)  String searchKey, @RequestParam(value="page", defaultValue = "1") int page,
                                                                     @RequestParam(value="limit", defaultValue = "25") int limit, @RequestParam Status status) {
        return contractService.getProvidersContractLists(providerUuid, searchKey, page, limit, status);
    }


    @GetMapping("/available-providers")
    @Operation(summary = "Get available providers", description = "Retrieves a list of providers available for contracting")
    @PreAuthorize("hasRole('Read-Provider-Contract')")
    public ResponseEntity<?> getAvailableProviders(
            @RequestParam(name = "search", required = false) String searchKey,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {

        Pageable pageable = PaginationUtils.paginateResource(page, limit, "providerName", "asc");
        return contractService.getAvailableProvidersForContract(searchKey, pageable);
    }

    @GetMapping("/provider/{providerUuid}/services")
    @Operation(summary = "Get provider services", description = "Retrieves a list of services offered by a specific provider")
    @PreAuthorize("hasRole('Read-Provider-Contract')")
    public ResponseEntity<?> getProviderServices(
            @PathVariable String providerUuid,
            @RequestParam(name = "search", required = false) String searchKey,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {

        Pageable pageable = PaginationUtils.paginateResource(page, limit, "serviceName", "asc");
        return contractService.getAvailableServicesForProvider(providerUuid, searchKey, pageable);
    }

    @PostMapping("/{contractUuid}/employee-groups")
    @Operation(summary = "Add employee groups to contract", description = "Adds employee groups to an existing contract")
    @PreAuthorize("hasRole('Create-Provider-Contract')")
    public ResponseEntity<?> addEmployeeGroups(
            @PathVariable String contractUuid,
            @Valid @RequestBody List<EmployeeGroupRequest> groups) {

        return contractService.addEmployeeGroupsToContract(contractUuid, groups);
    }

    @PostMapping("/{contractUuid}/service-group-assignments")
    @Operation(summary = "Assign services to groups", description = "Assigns services to employee groups within a contract")
    @PreAuthorize("hasRole('Create-Provider-Contract')")
    public ResponseEntity<?> assignServicesToGroups(
            @PathVariable String contractUuid,
            @Valid @RequestBody List<ContractServiceGroupAssignmentRequest> assignments) {

        return contractService.assignServicesToEmployeeGroups(contractUuid, assignments);
    }
}