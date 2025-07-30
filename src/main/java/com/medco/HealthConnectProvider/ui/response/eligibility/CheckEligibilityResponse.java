package com.medco.HealthConnectProvider.ui.response.eligibility;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CheckEligibilityResponse {
    private String insuredUuid;
    private String profilePicture;
    private String status;
    private String title;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String idNumber;
    private String insuranceId;
    private String insuredPhone;
    private String email;
    private LocalDateTime birthDate;
    private LocalDateTime beginDate;
    private LocalDateTime endDate;
    private String address1;
    private String address2;
    private String address3;
    private String position;
    private String branchOffice;
    private String state;
    private String country;
    private String payerName;
    private String payerPhone;
    private String providerName;
    private String providerPhone;
    private String institutionName;
    private String institutionPhone;
    private String payerProviderContractCode;
    private String payerProviderContractName;
    private LocalDateTime payerProviderContractBeginDate;
    private LocalDateTime payerProviderContractEndDate;
    private String payerInstitutionContractCode;
    private String payerInstitutionContractName;
    private LocalDateTime payerInstitutionContractBeginDate;
    private LocalDateTime payerInstitutionContractEndDate;
    private String gender;
}
