package com.medco.HealthConnectProvider.controller.contract;

import com.medco.HealthConnectProvider.services.contract.ContractService;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractDetailRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractRenewalRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractTerminationRequest;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractDetailResponse;
import com.medco.HealthConnectProvider.utils.paginationUtils.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/contracts")
@Tag(name = "Contract Detail Management", description = "APIs for managing contract details, approvals, renewals, and terminations")
public class ContractDetailController {

    @Autowired
    private ContractService contractService;

    @PostMapping("/{contractUuid}/services")
    @Operation(summary = "Add service to contract", description = "Adds a service with negotiated price to a contract")
    //@PreAuthorize("hasRole('Manage-Contract-Details')")
    public ResponseEntity<?> addServiceToContract(
            @PathVariable String contractUuid,
            @Valid @RequestBody ContractDetailRequest detailRequest) {
        return contractService.addServiceToContract(contractUuid, detailRequest);
    }

    @PutMapping("/services/{contractDetailUuid}")
    @Operation(summary = "Update contract detail", description = "Updates a service's negotiated price or assigned groups in a contract")
    //@PreAuthorize("hasRole('Manage-Contract-Details')")
    public ResponseEntity<?> updateContractDetail(
            @PathVariable String contractDetailUuid,
            @Valid @RequestBody ContractDetailRequest detailRequest) {
        return contractService.updateContractDetail(contractDetailUuid, detailRequest);
    }

    @DeleteMapping("/services/{contractDetailUuid}")
    @Operation(summary = "Remove service from contract", description = "Removes a service from a contract")
    //@PreAuthorize("hasRole('Manage-Contract-Details')")
    public ResponseEntity<?> removeServiceFromContract(@PathVariable String contractDetailUuid) {
        return contractService.removeServiceFromContract(contractDetailUuid);
    }

    @GetMapping("/{contractUuid}/services")
    @Operation(summary = "Get contract details", description = "Gets all services and their details for a contract")
    //@PreAuthorize("hasRole('View-Contract-Details')")
    public List<ContractDetailResponse> getContractDetails(
            @PathVariable String contractUuid,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {
        Pageable pageable = PaginationUtils.paginateResource(page, limit, "id", "desc");
        return contractService.getContractDetails(contractUuid, pageable);
    }

    @PostMapping("/{contractUuid}/submit")
    @Operation(summary = "Submit contract for approval", description = "Submits a contract for approval")
   // @PreAuthorize("hasRole('Submit-Contract')")
    public ResponseEntity<?> submitContractForApproval(@PathVariable String contractUuid) {
        return contractService.submitContractForApproval(contractUuid);
    }

    @PostMapping("/{contractUuid}/review")
    @Operation(summary = "Review contract", description = "Approves or rejects a contract")
   // @PreAuthorize("hasRole('Review-Contract')")
    public ResponseEntity<?> reviewContract(
            @PathVariable String contractUuid,
            @RequestParam boolean approved,
            @RequestParam(required = false) String reviewerComments) {
        return contractService.reviewContract(contractUuid, reviewerComments, approved);
    }

    @PostMapping("/{contractUuid}/renew")
    @Operation(summary = "Initiate contract renewal", description = "Creates a new contract as a renewal of an existing one")
    //@PreAuthorize("hasRole('Renew-Contract')")
    public ResponseEntity<?> initiateContractRenewal(
            @PathVariable String contractUuid,
            @Valid @RequestBody ContractRenewalRequest renewalRequest) {
        return contractService.initiateContractRenewal(contractUuid, renewalRequest);
    }

    @DeleteMapping("/renewals/{renewalUuid}")
    @Operation(summary = "Cancel renewal", description = "Cancels a contract renewal")
    //@PreAuthorize("hasRole('Renew-Contract')")
    public ResponseEntity<?> cancelRenewal(@PathVariable String renewalUuid) {
        return contractService.cancelRenewal(renewalUuid);
    }

    @PostMapping("/{contractUuid}/terminate")
    @Operation(summary = "Terminate contract", description = "Initiates termination of a contract")
    //@PreAuthorize("hasRole('Terminate-Contract')")
    public ResponseEntity<?> terminateContract(
            @PathVariable String contractUuid,
            @Valid @RequestBody ContractTerminationRequest terminationRequest) {
        return contractService.terminateContract(contractUuid, terminationRequest);
    }

    @PostMapping("/{contractUuid}/withdraw-termination")
    @Operation(summary = "Withdraw termination", description = "Withdraws a pending contract termination")
   // @PreAuthorize("hasRole('Terminate-Contract')")
    public ResponseEntity<?> withdrawTermination(@PathVariable String contractUuid) {
        return contractService.withdrawTermination(contractUuid);
    }

}