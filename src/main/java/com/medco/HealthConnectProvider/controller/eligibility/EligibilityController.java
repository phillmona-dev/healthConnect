//package com.medco.HealthConnectProvider.controller.eligibility;
//
//import com.medco.HealthConnectProvider.services.eligibility.EligibilityService;
//import com.medco.HealthConnectProvider.ui.request.eligibility.EligibilityCheckRequest;
//import com.medco.HealthConnectProvider.ui.response.eligibility.EligibilityResponse;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/v1/healthConnect/eligibility")
//@Tag(name = "Eligibility Verification", description = "APIs for verifying patient eligibility for services")
//public class EligibilityController {
//
//    @Autowired
//    private EligibilityService eligibilityService;
//
//    @PostMapping("/check/{providerUuid}")
//    @PreAuthorize("hasRole('Check-Eligibility')")
//    @Operation(summary = "Check patient eligibility", description = "Verifies if a patient is eligible for services based on their insurance and group membership")
//    public ResponseEntity<EligibilityResponse> checkEligibility(
//            @PathVariable String providerUuid,
//            @Valid @RequestBody EligibilityCheckRequest request) {
//        return eligibilityService.checkEligibility(providerUuid, request);
//    }
//}