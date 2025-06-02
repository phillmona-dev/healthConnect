package com.medco.HealthConnectProvider.ui.response.claims;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvidedServiceResponse {
    private String providedServiceUuid;
    private String serviceUuid;
    private String serviceName;
    private String serviceCode;
    private Double quantity;
    private Double unitPrice;
    private Double totalPrice;
    private String serviceCategory;
    private String serviceSubCategory;
    private BigDecimal negotiatedPrice;
}