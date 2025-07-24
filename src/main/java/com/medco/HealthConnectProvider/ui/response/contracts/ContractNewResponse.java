package com.medco.HealthConnectProvider.ui.response.contracts;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class ContractNewResponse {
    private String contractUuid;
    private String contractName;
    private String description;
    private String contractCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private String payerUuid;
    private String payerName;
    private String providerUuid;
    private String providerName;
    private Status status;
}