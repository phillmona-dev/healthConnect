package com.medco.HealthConnectProvider.services.eligibility;

import com.medco.HealthConnectProvider.ui.request.eligibility.EligibilityCheckRequest;
import com.medco.HealthConnectProvider.ui.response.eligibility.EligibilityResponse;
import org.springframework.http.ResponseEntity;

public interface EligibilityService {
    ResponseEntity<EligibilityResponse> checkEligibility(String providerUuid, EligibilityCheckRequest request);
}