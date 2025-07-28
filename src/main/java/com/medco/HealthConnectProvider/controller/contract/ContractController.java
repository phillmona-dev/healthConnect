package com.medco.HealthConnectProvider.controller.contract;

import com.medco.HealthConnectProvider.annotation.RequiresApiKey;
import com.medco.HealthConnectProvider.services.contract.ContractService;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.group.EmployeeGroupRequest;
import com.medco.HealthConnectProvider.ui.request.contract.AddInsuredToContractRequest;
import com.medco.HealthConnectProvider.ui.request.contract.ContractFilterRequest;
import com.medco.HealthConnectProvider.ui.request.contract.ContractStatusUpdateRequest;
import com.medco.HealthConnectProvider.ui.request.contract.CreateActiveContractRequest;
import com.medco.HealthConnectProvider.ui.response.contracts.*;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.paginationUtils.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
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
    public ResponseEntity<ContractResponse> createContract(@Valid @RequestBody ContractRequest contractRequest) {
        return contractService.createContract(contractRequest);
    }

    //create contract for hcPayer
    @RequiresApiKey
    @PostMapping("/hcPayer/create-active-contract")
    @Operation(summary = "Create an active contract for hc payer", description = "Creates a new active contract, creating a payer if it doesn't exist")
    public ResponseEntity<ContractNewResponse> createActiveContract(@Valid @RequestBody CreateActiveContractRequest request) {
        return ResponseEntity.ok(contractService.createActiveContract(request));
    }

    @PutMapping(path="/{contractUuid}")
    @Operation(summary = "Update contract", description = "Updates an existing contract by UUID")
    //@PreAuthorize("hasRole('Update-Provider-Contract')")
    public ResponseEntity<?> updateContract(@PathVariable String contractUuid, @Valid @RequestBody ContractRequest contractRequest) {
        return contractService.updateContract(contractUuid, contractRequest);
    }

    @GetMapping(path="/{contractUuid}")
    @Operation(
            summary = "Retrieve detailed contract information with search capability",
            description = "Fetches comprehensive contract details based on the provided contract UUID. " +
                    "The response is tailored according to the user type (payer or provider). " +
                    "An optional search key can be used to filter results across the following fields:\n" +
                    "- For insured persons: full name, membership number, and phone number\n" +
                    "- For dependants: full name, phone number, and relationship type\n"
    )
    public ContractResponse getContract(
            @PathVariable String contractUuid,
            @RequestParam String userType,
            @RequestParam(required = false) String searchKey) {
        return contractService.getContract(contractUuid, userType, searchKey);
    }

    @PutMapping(path="/approve/{payerProviderContractUuid}")
    @Operation(summary = "Approve contract", description = "Approves a contract by UUID")
    // @PreAuthorize("hasRole('Approve-Provider-Contract')")
    //@PreAuthorize("hasRole('Read-Provider-Contract')")
    public ResponseEntity<?> approveContract(@PathVariable String payerProviderContractUuid) {
        return contractService.approveContract(payerProviderContractUuid);
    }

    @PutMapping(path="/agreed/{payerProviderContractUuid}")
    @Operation(summary = "Provider agreement response", description = "Updates a contract with provider agreement status and remarks")
    // @PreAuthorize("hasRole('Approve-Provider-Contract')")
    //@PreAuthorize("hasRole('Read-Provider-Contract')")
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
                                                                          @RequestParam(required = false) String status) {
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
    // @PreAuthorize("hasRole('Read-Provider-Contract')")
    public ResponseEntity<?> getAvailableProviders(
            @RequestParam(name = "search", required = false) String searchKey,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {

        Pageable pageable = PaginationUtils.paginateResource(page, limit, "providerName", "asc");
        return contractService.getAvailableProvidersForContract(searchKey, pageable);
    }

    @GetMapping("/provider/{providerUuid}/services")
    @Operation(summary = "Get provider services", description = "Retrieves a list of services offered by a specific provider")
    // @PreAuthorize("hasRole('Read-Provider-Contract')")
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
    //@PreAuthorize("hasRole('Create-Provider-Contract')")
    public ResponseEntity<?> addEmployeeGroups(
            @PathVariable String contractUuid,
            @Valid @RequestBody List<EmployeeGroupRequest> groups) {

        return contractService.addEmployeeGroupsToContract(contractUuid, groups);
    }

    @PostMapping("/{groupUuid}/assign-services")
    @Operation(summary = "Assign services to group", description = "Assigns multiple services to an employee group")
    public ResponseEntity<AssignServicesToGroupResponse> assignServicesToGroup(
            @PathVariable String groupUuid,
            @Valid @RequestBody
            @Schema(name = "contractDetailUuids", description = "List of contract detail UUIDs to be assigned to the group")
            List<String> contractDetailUuids) {
        return contractService.assignServicesToGroup(groupUuid, contractDetailUuids);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getContracts(
            @RequestParam(required = false) String contractNumber,
            @RequestParam(required = false) String contractName,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String payerUuid,
            @RequestParam(required = false) String providerUuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateTo,
            @RequestParam(required = false) String preparedBy,
            @RequestParam(required = false) Boolean isDeleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "startDate") String[] sort) {

        ContractFilterRequest filter = ContractFilterRequest.builder()
                .contractNumber(contractNumber)
                .contractName(contractName)
                .status(status)
                .payerUuid(payerUuid)
                .providerUuid(providerUuid)
                .startDateFrom(startDateFrom)
                .startDateTo(startDateTo)
                .endDateFrom(endDateFrom)
                .endDateTo(endDateTo)
                .preparedBy(preparedBy)
                .isDeleted(isDeleted)
                .build();

        int zeroBasedPage = Math.max(0, page - 1);

        Pageable pageable = PageRequest.of(zeroBasedPage, size, Sort.by(sort));

        return contractService.getFilteredContracts(filter, pageable, page);

    }

    private Sort getSort(String[] sort) {
        List<Sort.Order> orders = new ArrayList<>();

        if (sort[0].contains(",")) {
            for (String sortOrder : sort) {
                String[] sortParams = sortOrder.split(",");
                orders.add(new Sort.Order(getSortDirection(sortParams[1]), sortParams[0]));
            }
        } else {
            orders.add(new Sort.Order(getSortDirection(sort[1]), sort[0]));
        }

        return Sort.by(orders);
    }

    private Sort.Direction getSortDirection(String direction) {
        return direction.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
    }

    @GetMapping("/{contractHeaderUuid}/detailed")
    @Operation(summary = "Get detailed contract information", description = "Retrieves detailed information about a contract, including services, insured persons, and dependants")
    public ResponseEntity<DetailedContractResponse> getDetailedContract(
            @PathVariable String contractHeaderUuid,
            @RequestParam String userType) {

        DetailedContractResponse detailedContract = contractService.getDetailedContract(contractHeaderUuid, userType);
        return ResponseEntity.ok(detailedContract);
    }

    @PostMapping("/{contractUuid}/add-insured")
    @Operation(summary = "Add insured and dependants to contract",
            description = "Adds specified insured persons and their dependants to the contract")
    public ResponseEntity<?> addInsuredToContract(
            @PathVariable String contractUuid,
            @Valid @RequestBody AddInsuredToContractRequest request) {
        return contractService.addInsuredToContract(contractUuid, request);
    }

    @GetMapping("/eligible-services")
    @Operation(summary = "Get eligible services for a contract and insured/dependant with advanced search")
    public ResponseEntity<List<EligibleServiceResponse>> getEligibleServices(
            @RequestParam String contractHeaderUuid,
            @RequestParam(required = false) String insuredUuid,
            @RequestParam(required = false) String dependantUuid,
            @RequestParam(required = false) String searchKey) {
        return contractService.getEligibleServices(contractHeaderUuid, insuredUuid, dependantUuid, searchKey);
    }

    @PutMapping("/{contractUuid}/status")
    //@PreAuthorize("hasRole('ROLE_PAYER') or hasRole('ROLE_PROVIDER')")
    @Operation(
            summary = "Update contract status",
            description = "Updates the status of a contract based on the action performed by either the payer or provider. " +
                    "The workflow is as follows:\n" +
                    "1. Provider can APPROVE or REJECT a PENDING contract\n" +
                    "2. Payer can ACTIVATE or REJECT an APPROVED contract\n" +
                    "3. Payer can RESUBMIT a REJECTED contract"
    )
    public ResponseEntity<?> updateContractStatus(
            @Parameter(description = "UUID of the contract to update")
            @PathVariable String contractUuid,
            @Parameter(description = "Contract status update request containing the action and additional information")
            @Valid @RequestBody ContractStatusUpdateRequest updateRequest) {
        return contractService.updateContractStatus(contractUuid, updateRequest);
    }

    @DeleteMapping("/soft-delete/{contractUuid}")
    //@PreAuthorize("hasRole('ROLE_PAYER')")
    @Operation(
            summary = "Soft delete a rejected contract",
            description = "Marks a rejected contract as deleted without removing it from the database. Only accessible to payers."
    )
    public ResponseEntity<?> softDeleteRejectedContract(
            @Parameter(description = "UUID of the contract to soft delete")
            @PathVariable String contractUuid) {
        return contractService.softDeleteRejectedContract(contractUuid);
    }

    @PostMapping("/create-kenema-contract")
    @Operation(summary = "Create new contracts for Kenema provider", description = "Creates new active contracts between the specified payers and the Kenema provider")
    public ResponseEntity<List<ContractResponse>> createKenemaContracts(@RequestBody List<String> payerUuids) {
        return contractService.createKenemaContracts(payerUuids);
    }

}
