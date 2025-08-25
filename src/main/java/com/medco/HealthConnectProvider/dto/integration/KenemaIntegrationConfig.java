package com.medco.HealthConnectProvider.dto.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KenemaIntegrationConfig {

    private String contractHeaderUuid;
    private boolean isInsurance;
    private String packageUuid;
    private String dependantUuid;
    private String serviceId;
    
    private String payerUuid;
    private String payerType;
}
