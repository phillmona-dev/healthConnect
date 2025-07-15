package com.medco.HealthConnectProvider.ui.response.payer;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PolicyHolderListResponse {

    private String payerUuid;
    private String payerName;
    private String description;
    private String telephone;
    private String tinNumber;
    private String category;
    private String payerInsuranceNumber;
    private String address1;
    private String address2;
    private String address3;
    private String state;
    private String country;
    private Status status;
    private String referralType;
    private String referredBy;

}
