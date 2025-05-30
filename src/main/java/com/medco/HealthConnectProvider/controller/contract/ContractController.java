package com.medco.HealthConnectProvider.controller.contract;

import com.medco.HealthConnectProvider.services.contract.ContractService;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractRequest;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractListPayerResponse;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.paginationUtils.PaginationUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payer/healthConnect/payer-provider-contract")
@SecurityRequirement(name = "bearerAuth")

public class ContractController {


    @Autowired
    ContractService contractService;

    @PostMapping
//	  @PreAuthorize("hasRole('Create-Provider-Contract')")
    public ResponseEntity<?> createContract(@Valid @RequestBody ContractRequest contractRequest) {
        return contractService.createContract(contractRequest);

    }

    @PutMapping(path="/{contractUuid}")
//	  @PreAuthorize("hasRole('Update-Provider-Contract')")
    public ResponseEntity<?> updateContract(@PathVariable String contractUuid, @Valid @RequestBody ContractRequest contractRequest) {
        return contractService.updateContract(contractUuid, contractRequest);
    }


    @GetMapping(path="/{contractUuid}")
//	  @PreAuthorize("hasRole('Read-Provider-Contract')")
    public ContractResponse getContract(@PathVariable String contractUuid) {
        return contractService.getContract(contractUuid);
    }


    @PutMapping(path="/approve/{payerProviderContractUuid}")
    // @PreAuthorize("hasRole('Approve-Provider-Contract')")
    @PreAuthorize("hasRole('Read-Provider-Contract')")
    public ResponseEntity<?> approveContract(@PathVariable String payerProviderContractUuid) {
        return contractService.approveContract(payerProviderContractUuid);
    }

    @PutMapping(path="/agreed/{payerProviderContractUuid}")
    // @PreAuthorize("hasRole('Approve-Provider-Contract')")
    @PreAuthorize("hasRole('Read-Provider-Contract')")
    public ResponseEntity<?> providerAgreed(@PathVariable String payerProviderContractUuid,
                                            @RequestParam String status,
                                            @RequestParam String remark) {
        return contractService.payerAgreementResponse(payerProviderContractUuid,status,remark);
    }

    @DeleteMapping(path="/{contractUuid}")
//	  @PreAuthorize("hasRole('Delete-Provider-Contracts')")
    public ResponseEntity<?> deleteContract(@PathVariable String contractUuid) {
        return contractService.deleteContract(contractUuid);
    }

    @GetMapping("/provider/lists")
//	@PreAuthorize("hasRole('Read-Provider-Contracts')")
    public List<ContractListPayerResponse> getPayerProvidersContractLists(@RequestParam(name = "search", required = false)  String searchKey,
                                                                          @RequestParam(value="page", defaultValue = "1") int page,
                                                                          @RequestParam(value="limit", defaultValue = "25") int limit,
                                                                          @RequestParam String status) {
        Pageable pageable = PaginationUtils.paginateResource(page,limit,"id","desc");
        return contractService.getPayerProvidersContractLists(searchKey,pageable, status);
    }

    @GetMapping("/provider/contract/lists")
//	@PreAuthorize("hasRole('Read-Provider-Contracts')")
    public List<ContractListPayerResponse> getProvidersContractLists(@RequestParam  String providerUuid, @RequestParam(name = "search", required = false)  String searchKey, @RequestParam(value="page", defaultValue = "1") int page,
                                                                     @RequestParam(value="limit", defaultValue = "25") int limit, @RequestParam Status status) {
        return contractService.getProvidersContractLists(providerUuid, searchKey, page, limit, status);
    }


}
