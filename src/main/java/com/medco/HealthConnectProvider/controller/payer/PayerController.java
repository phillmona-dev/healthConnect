package com.medco.HealthConnectProvider.controller.payer;

import com.medco.HealthConnectProvider.services.payer.PayerService;
import com.medco.HealthConnectProvider.ui.request.auth.password.payer.PayerRequest;
import com.medco.HealthConnectProvider.ui.response.payer.PayerProviderResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PayerResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PolicyHolderListResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/payer")
public class PayerController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    PayerService payerService;

    @PostMapping
    //@PreAuthorize("hasRole('Create-Institution')")
    public PayerResponse createInstitution(@Valid @RequestBody PayerRequest institutionRequest) {
        return payerService.createPayer(institutionRequest);

    }

    @PutMapping(path="/{institutionUuid}")
    //@PreAuthorize("hasRole('Update-Institution')")
    public PayerResponse updateInstitution(@PathVariable String institutionUuid, @Valid @RequestBody PayerRequest institutionRequest) {
        return payerService.updatePayer(institutionUuid, institutionRequest);
    }

    @PutMapping(path="/set-institution-insurance-number/{institutionUuid}")
    public ResponseEntity<?> setInstitutionInsuranceNumber(@PathVariable String payerUuid, @Valid @RequestParam String payerInsuranceNumber) {
        return payerService.setPayerInsuranceNumber(payerUuid, payerInsuranceNumber);
    }
    @PutMapping(path="/updateInstitutionStatus/{institutionUuid}")
    public ResponseEntity<?> updateInstitutionStatus(@PathVariable String payerUuid,  @RequestParam Status payerStatus) {
        return payerService.updatePayerStatus(payerUuid, payerStatus);
    }

    @GetMapping(path="/{payerUuid}")
    //@PreAuthorize("hasRole('Read-Payer')")
    public PayerResponse getPayer(@PathVariable String payerUuid) {
        return payerService.getPayer(payerUuid);
    }

    @GetMapping("/list")
   // @PreAuthorize("hasRole('Read-Payers')")
    public List<PayerResponse> getPayers(@RequestParam(value="search", defaultValue="", required=false) String search,
                                         @RequestParam(value="page", defaultValue = "1") int page,
                                         @RequestParam(value="limit", defaultValue = "25") int limit,
                                         @RequestParam Status status){
        return payerService.getPayers(search, page,limit, status);
    }

    @GetMapping("/policy-holders/list")
    //@PreAuthorize("hasRole('Read-Institutions')")
    public List<PolicyHolderListResponse> getPolicyHolders(@RequestParam(value="search", defaultValue="", required=false) String search, @RequestParam(value="page", defaultValue = "1") int page,
                                                           @RequestParam(value="limit", defaultValue = "25") int limit, @RequestParam Status status){
        return payerService.getPolicyHolders(search, page,limit, status);
    }

    @GetMapping("/provider-assigned/policy-holders/list")
    public List<PayerProviderResponse> getProviderPolicyHolders(@RequestParam String providerUuid, String payerUuid){
        return payerService.getProviderPolicyHolders(providerUuid, payerUuid);
    }

    @DeleteMapping(path="/{payerUuid}")
    //@PreAuthorize("hasRole('Delete-Payer')")
    public ResponseEntity<?> deletePayer(@PathVariable String payerUuid) {
        return payerService.deletePayer(payerUuid);
    }
}
