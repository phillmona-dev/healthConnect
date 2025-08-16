package com.medco.HealthConnectProvider.services.integration;

import com.medco.HealthConnectProvider.dto.integration.ExternalInsuranceDispensingRequest;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensingItem;

import java.util.List;

public interface ExternalInsuranceService {

    /**
     * Send medication dispensing items to external insurance system
     */
    void sendDispensingToExternalSystem(List<MedicationDispensingItem> dispensingItems,
                                       String packageUuid,
                                       String serviceId,
                                       String dispensingUuid,
                                       String contractHeaderUuid);
}
