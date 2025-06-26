package com.medco.HealthConnectProvider.ui.response.claims;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class ItemResponse {
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
