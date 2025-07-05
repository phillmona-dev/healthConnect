package com.medco.HealthConnectProvider.ui.response.contracts;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class EligibleServiceResponse {
    private String contractHeaderUuid;
    private String contractName;
    private String contractDetailUuid;
    private String serviceUuid;
    private String serviceName;
    private String serviceCode;
    private BigDecimal negotiatedPrice;
    private String status;
    private String drugUuid;
    private String drugName;
}
