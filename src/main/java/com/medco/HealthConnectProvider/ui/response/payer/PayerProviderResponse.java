package com.medco.HealthConnectProvider.ui.response.payer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PayerProviderResponse {
    private String payerUuid;
    private String payerName;
    private String payerInsuranceNumber;
}
