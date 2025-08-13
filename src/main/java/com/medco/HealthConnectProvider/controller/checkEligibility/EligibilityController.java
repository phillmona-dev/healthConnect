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
    public ResponseEntity<List<InstitutionResponse>> getInstitutions(
            @RequestParam String contractUuid) {
        List<InstitutionResponse> institutions = institutionService.getInstitutions(contractUuid);
        return ResponseEntity.ok(institutions);
    }

    @GetMapping("/insuredEligibility")
    @Operation(
            summary = "Check insured eligibility",
            description = "Checks the eligibility of insured individuals",
            parameters = {
                    @Parameter(name = "contractUuid", description = "Contract UUID", required = true),
                    @Parameter(name = "institutionUuid", description = "Institution UUID", required = true),
                    @Parameter(name = "search", description = "Optional search criteria", required = false)
            }
    )
    public ResponseEntity<List<CheckEligibilityResponse>> checkEligibility(
            @RequestParam String contractUuid,
            @RequestParam String institutionUuid,
            @RequestParam(required = false) String search) {
        List<CheckEligibilityResponse> responses = eligibilityService.checkEligibility(
                contractUuid,
                institutionUuid,
                search
        );
        return ResponseEntity.ok(responses);
    }
}
