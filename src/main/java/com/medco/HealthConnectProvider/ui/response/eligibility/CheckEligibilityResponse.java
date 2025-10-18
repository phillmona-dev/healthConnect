package com.medco.HealthConnectProvider.ui.response.eligibility;

import com.medco.HealthConnectProvider.utils.enums.Relationship;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

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
    private Date birthDate;
    private Date beginDate;
    private Date endDate;
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
    private Date payerProviderContractBeginDate;
    private Date payerProviderContractEndDate;
    private String payerInstitutionContractCode;
    private String payerInstitutionContractName;
    private Date payerInstitutionContractBeginDate;
    private Date payerInstitutionContractEndDate;
    private String gender;


    private List<DependantResponse> dependantResponses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DependantResponse {

        private String insuredPersonUuid;
        private String dependantUuid;
        private String title;
        private String firstName;
        private String fatherName;
        private String grandFatherName;
        private String Gender;
        private Date birthDate;

        @Enumerated(EnumType.STRING)
        private Relationship relationship;

        private String phone;
        private Status status;
        private String profile;

    }
}
