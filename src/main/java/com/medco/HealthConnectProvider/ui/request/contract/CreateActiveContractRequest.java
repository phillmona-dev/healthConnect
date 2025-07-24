package com.medco.HealthConnectProvider.ui.request.contract;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class CreateActiveContractRequest {
    private String contractUuid;
    private String contractName;
    private String description;
    private String contractCode;
    private Date beginDate;
    private Date endDate;
    private String payerUuid;
    private String addedBy;
    private String payerName;
    private String payerPhone;
    private String providerUuid;
    private String payerSubCity;
    private String payerCategory;
    private String payerEmail;
}
