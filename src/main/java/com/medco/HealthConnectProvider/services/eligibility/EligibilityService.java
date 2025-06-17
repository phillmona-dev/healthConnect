package com.medco.HealthConnectProvider.services.eligibility;

import com.medco.HealthConnectProvider.ui.request.eligibility.EligibilityCheckRequest;
import com.medco.HealthConnectProvider.ui.response.eligibility.EligibilityResponse;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredSearchResponse;
import org.springframework.http.ResponseEntity;

public interface EligibilityService {
    ResponseEntity<?> checkEligibility(String providerUuid, EligibilityCheckRequest request);

    ResponseEntity<EligibilityResponse> checkEligibilityForInsured(String providerUuid, InsuredSearchResponse insured, String serviceUuid);
}