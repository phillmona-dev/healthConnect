package com.medco.HealthConnectProvider.ui.response.payer;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayerResponse {
    private String payerUuid;
    private String email;
    private String payerName;
    private String description;
    private String telephone;
    private Long tinNumber;
    private String category;
    private String payerInsuranceNumber;
    private String address1;
    private String address2;
    private String address3;
    private String state;
    private String country;
    private double latitude;
    private double longitude;

    private String referralType;
    private String referredBy;

    private String roleUuid;

    private String dependantCoverage;

    private Status status;
    private long totalPages;

    private String logoPath;
    private String logoBase64;
    private Long totalContracts;
}
