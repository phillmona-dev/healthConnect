package com.medco.HealthConnectProvider.services.eligibility;

import com.medco.HealthConnectProvider.ui.response.eligibility.InstitutionResponse;

import java.util.List;

public interface InstitutionService {

    List<InstitutionResponse> getInstitutions(String contractUuid);
}
