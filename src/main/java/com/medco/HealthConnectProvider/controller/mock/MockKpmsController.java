package com.medco.HealthConnectProvider.controller.mock;

import com.medco.HealthConnectProvider.ui.request.eligibility.EligibilityCheckRequest;
import com.medco.HealthConnectProvider.ui.response.eligibility.EligibilityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/mock-kpms")
@Tag(name = "Mock KPMS", description = "Mock APIs for Kenema Pharmacy Management System")
public class MockKpmsController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${healthconnect.api.base-url}")
    private String healthConnectBaseUrl;

    @PostMapping("/dispense")
    @Operation(summary = "Mock medication dispensing", description = "Calls the real medication dispensing endpoint")
    public ResponseEntity<?> mockDispense(@RequestBody Map<String, Object> request) {
        String url = healthConnectBaseUrl + "/api/v1/healthConnect/integration/pharmacy/dispensing";
        return restTemplate.postForEntity(url, request, Map.class);
    }

    @PostMapping("/eligibility")
    @Operation(summary = "Mock eligibility check", description = "Calls the real eligibility check endpoint")
    public ResponseEntity<?> mockEligibilityCheck(@RequestParam String providerUuid,
                                                  @RequestBody EligibilityCheckRequest request) {
        String url = healthConnectBaseUrl + "/api/v1/healthConnect/integration/pharmacy/check/" + providerUuid;
        return restTemplate.postForEntity(url, request, EligibilityResponse.class);
    }

    @GetMapping("/medication")
    @Operation(summary = "Mock medication info", description = "Calls the real medication info fetch endpoint")
    public ResponseEntity<?> mockMedicationInfo(@RequestParam String medicationCode,
                                                @RequestParam String providerUuid) {
        String url = healthConnectBaseUrl + "/api/v1/healthConnect/integration/pharmacy/medication?medicationCode=" + medicationCode + "&providerUuid=" + providerUuid;
        return restTemplate.getForEntity(url, Map.class);
    }

    // You might need this for more complex scenarios
    private HttpEntity<?> createHttpEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        // Add any necessary headers here
        return new HttpEntity<>(body, headers);
    }
}