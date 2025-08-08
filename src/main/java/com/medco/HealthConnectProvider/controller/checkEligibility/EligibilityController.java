package com.medco.HealthConnectProvider.controller.checkEligibility;

import com.medco.HealthConnectProvider.services.eligibility.CheckEligibilityService;
import com.medco.HealthConnectProvider.services.eligibility.InstitutionService;
import com.medco.HealthConnectProvider.ui.response.eligibility.CheckEligibilityResponse;
import com.medco.HealthConnectProvider.ui.response.eligibility.InstitutionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/eligibility")
public class EligibilityController {

    @Autowired
    private CheckEligibilityService eligibilityService;

    @Autowired
    private InstitutionService institutionService;

    @GetMapping("/institutions")
    @Operation(
            summary = "Get all institutions",
            description = "Retrieves a list of all registered institutions in the system."

    )
    public ResponseEntity<List<InstitutionResponse>> getInstitutions() {
        List<InstitutionResponse> institutions = institutionService.getInstitutions();
        return ResponseEntity.ok(institutions);
    }

    @GetMapping("/insuredEligibility")
    @Operation(
            summary = "Check insured eligibility",
            description = "Checks the eligibility of insured individuals based on the provided institution UUID and search criteria.",
            tags = {"Eligibility"},
            parameters = {
                    @Parameter(name = "institutionUuid", description = "UUID of the institution", required = true),
                    @Parameter(name = "search", description = "Search criteria for insured individuals (e.g., name, ID, policy number)", required = true)
            }
    )
    public ResponseEntity<List<CheckEligibilityResponse>> checkEligibility(
            @RequestParam String institutionUuid,
            @RequestParam String search) {
        List<CheckEligibilityResponse> responses = eligibilityService.checkEligibility(institutionUuid, search);
        return ResponseEntity.ok(responses);
    }

}
