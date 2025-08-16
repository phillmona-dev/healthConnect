package com.medco.HealthConnectProvider.dto.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for Kenema pharmacy integration
 * Contains additional fields resolved from your system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KenemaIntegrationConfig {

    private String contractHeaderUuid;
    private boolean isInsurance;
    private String packageUuid;
    private String dependantUuid;
    private String serviceId; // Default serviceId for medications if not specified per item
    
    // Additional fields for processing
    private String payerUuid;
    private String payerType; // To determine if it's insurance or not
}
