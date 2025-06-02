package com.medco.HealthConnectProvider.ui.response.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvidedServiceResponse {

    private String providedServiceUuid;
    private String claimUuid;
    private String contractDetailUuid;
    private String serviceName;
    private String serviceCode;
    private Double quantity;
    private Double unitPrice;
    private Double totalPrice;
}