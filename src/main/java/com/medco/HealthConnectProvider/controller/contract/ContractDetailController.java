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
import org.springframework.security.access.prepost.PreAuthorize;
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
//    @PreAuthorize("hasRole('Manage-Contract-Details')")
    public ResponseEntity<?> addServiceToContract(

            @PathVariable String contractUuid,
            @Valid @RequestBody ContractDetailRequest detailRequest) {
        System.out.println( "in the add service to contract controller ");
        return contractService.addServiceToContract(contractUuid, detailRequest);
    }

    @PutMapping("/services/{contractDetailUuid}")
    @Operation(summary = "Update contract detail", description = "Updates a service's negotiated price or assigned groups in a contract")
<<<<<<< HEAD
//    @PreAuthorize("hasRole('Manage-Contract-Details')")
=======
    //@PreAuthorize("hasRole('Manage-Contract-Details')")
    public ResponseEntity<?> updateContractDetail(
    }

    @DeleteMapping("/services/{contractDetailUuid}")
    @Operation(summary = "Remove service from contract", description = "Removes a service from a contract")
<<<<<<< HEAD
//    @PreAuthorize("hasRole('Manage-Contract-Details')")
=======
    //@PreAuthorize("hasRole('Manage-Contract-Details')")
>>>>>>> 9a83eb7d0f66755b562ce9359eefd043570a1929
    public ResponseEntity<?> removeServiceFromContract(@PathVariable String contractDetailUuid) {
        return contractService.removeServiceFromContract(contractDetailUuid);
    }

    @GetMapping("/{contractUuid}/services")
    @Operation(summary = "Get contract details", description = "Gets all services and their details for a contract")
<<<<<<< HEAD
//    @PreAuthorize("hasRole('View-Contract-Details')")
=======
    //@PreAuthorize("hasRole('View-Contract-Details')")
>>>>>>> 9a83eb7d0f66755b562ce9359eefd043570a1929
    public List<ContractDetailResponse> getContractDetails(
            @PathVariable String contractUuid,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {
        Pageable pageable = PaginationUtils.paginateResource(page, limit, "id", "desc");
        return contractService.getContractDetails(contractUuid, pageable);
    }

    @PostMapping("/{contractUuid}/submit")
    @Operation(summary = "Submit contract for approval", description = "Submits a contract for approval")
<<<<<<< HEAD
//    @PreAuthorize("hasRole('Submit-Contract')")
=======
   // @PreAuthorize("hasRole('Submit-Contract')")
>>>>>>> 9a83eb7d0f66755b562ce9359eefd043570a1929
    public ResponseEntity<?> submitContractForApproval(@PathVariable String contractUuid) {
        return contractService.submitContractForApproval(contractUuid);
    }

    @PostMapping("/{contractUuid}/review")
    @Operation(summary = "Review contract", description = "Approves or rejects a contract")
<<<<<<< HEAD
//    @PreAuthorize("hasRole('Review-Contract')")
=======
   // @PreAuthorize("hasRole('Review-Contract')")
>>>>>>> 9a83eb7d0f66755b562ce9359eefd043570a1929
    public ResponseEntity<?> reviewContract(
            @PathVariable String contractUuid,
            @RequestParam boolean approved,
            @RequestParam(required = false) String reviewerComments) {
        return contractService.reviewContract(contractUuid, reviewerComments, approved);
    }

    @PostMapping("/{contractUuid}/renew")
    @Operation(summary = "Initiate contract renewal", description = "Creates a new contract as a renewal of an existing one")
<<<<<<< HEAD
//    @PreAuthorize("hasRole('Renew-Contract')")
=======
    //@PreAuthorize("hasRole('Renew-Contract')")
>>>>>>> 9a83eb7d0f66755b562ce9359eefd043570a1929
    public ResponseEntity<?> initiateContractRenewal(
            @PathVariable String contractUuid,
            @Valid @RequestBody ContractRenewalRequest renewalRequest) {
        return contractService.initiateContractRenewal(contractUuid, renewalRequest);
    }

    @DeleteMapping("/renewals/{renewalUuid}")
    @Operation(summary = "Cancel renewal", description = "Cancels a contract renewal")
<<<<<<< HEAD
//    @PreAuthorize("hasRole('Renew-Contract')")
=======
    //@PreAuthorize("hasRole('Renew-Contract')")
>>>>>>> 9a83eb7d0f66755b562ce9359eefd043570a1929
    public ResponseEntity<?> cancelRenewal(@PathVariable String renewalUuid) {
        return contractService.cancelRenewal(renewalUuid);
    }

    @PostMapping("/{contractUuid}/terminate")
    @Operation(summary = "Terminate contract", description = "Initiates termination of a contract")
<<<<<<< HEAD
//    @PreAuthorize("hasRole('Terminate-Contract')")
=======
    //@PreAuthorize("hasRole('Terminate-Contract')")
>>>>>>> 9a83eb7d0f66755b562ce9359eefd043570a1929
    public ResponseEntity<?> terminateContract(
            @PathVariable String contractUuid,
            @Valid @RequestBody ContractTerminationRequest terminationRequest) {
        return contractService.terminateContract(contractUuid, terminationRequest);
    }

    @PostMapping("/{contractUuid}/withdraw-termination")
    @Operation(summary = "Withdraw termination", description = "Withdraws a pending contract termination")
<<<<<<< HEAD
//    @PreAuthorize("hasRole('Terminate-Contract')")
=======
   // @PreAuthorize("hasRole('Terminate-Contract')")
>>>>>>> 9a83eb7d0f66755b562ce9359eefd043570a1929
    public ResponseEntity<?> withdrawTermination(@PathVariable String contractUuid) {
        return contractService.withdrawTermination(contractUuid);
    }
}