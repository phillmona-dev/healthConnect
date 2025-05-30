package com.medco.HealthConnectProvider.ui.response.contracts;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractListPayerResponse {
    private String payerProviderContractUuid;
    private String contractName;
    private String contractCode;
    private String description;
    private String providerName;
    private String providerPhone;
    private String providerEmail;
    private String providerUuid;

    private Date beginDate;
    private Date endDate;
    private String status;
}