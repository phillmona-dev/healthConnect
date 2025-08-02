package com.medco.HealthConnectProvider.ui.response.provider;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProviderSummary {
    private String providerUuid;
    private String providerName;
    private long totalClaims;
    private long totalServices;
}
