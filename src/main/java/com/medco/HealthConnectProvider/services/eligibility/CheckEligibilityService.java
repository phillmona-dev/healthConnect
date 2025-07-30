package com.medco.HealthConnectProvider.services.eligibility;

import com.medco.HealthConnectProvider.ui.response.eligibility.CheckEligibilityResponse;
import com.medco.HealthConnectProvider.ui.response.eligibility.InstitutionResponse;

import java.util.List;

public interface CheckEligibilityService {

    List<CheckEligibilityResponse> checkEligibility(String institutionUuid, String search);

}
