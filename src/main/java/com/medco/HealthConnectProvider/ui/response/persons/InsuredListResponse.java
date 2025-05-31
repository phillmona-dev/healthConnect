package com.medco.HealthConnectProvider.ui.response.persons;

import java.util.Date;
import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InsuredListResponse {

    private String insuredUuid;
    private String payerInstitutionContractName;
    private String payerInstitutionContractCode;

    private LocalDate payerInstitutionContractBeginDate; // Changed to LocalDate
    private LocalDate payerInstitutionContractEndDate;   // Changed to LocalDate

    private String payerProviderContractCode;
    private String payerProviderContractName;

    private LocalDate payerProviderContractBeginDate;    // Changed to LocalDate
    private LocalDate payerProviderContractEndDate;      // Changed to LocalDate

    private String payerName;
    private String payerPhone;
    private String providerPhone;
    private String providerName;
    private String institutionPhone;
    private String institutionName;

    private String email;
    private String title;
    private String firstName;

    private String fatherName;
    private String grandFatherName;
    private String gender;

    private Date birthDate;
    private String insuredPhone;
    private String branchOffice;

    private String position;
    private String idNumber;
    private String insuranceId;

    private String profilePicture;
    private double premium;
    private double remainingAmount;

    private String address1;
    private String address2;
    private String address3;
    private String state;
    private String country;

    private Date beginDate;
    private Date endDate;
    private String status;

    // Constructor matching the JPQL query parameters
    public InsuredListResponse(
            String insuredUuid,
            String payerInstitutionContractName,
            String payerInstitutionContractCode,
            LocalDate payerInstitutionContractBeginDate,
            LocalDate payerInstitutionContractEndDate,
            String payerProviderContractCode,
            String payerProviderContractName,
            LocalDate payerProviderContractBeginDate,
            LocalDate payerProviderContractEndDate,
            String payerName,
            String payerPhone,
            String providerPhone,
            String providerName,
            String institutionPhone,
            String institutionName,
            String email,
            String title,
            String firstName,
            String fatherName,
            String grandFatherName,
            String gender,
            Date birthDate,
            String insuredPhone,
            String branchOffice,
            String position,
            String idNumber,
            String insuranceId,
            String profilePicture,
            double premium,
            double remainingAmount,
            String address1,
            String address2,
            String address3,
            String state,
            String country,
            Date beginDate,
            Date endDate,
            String status) {
        this.insuredUuid = insuredUuid;
        this.payerInstitutionContractName = payerInstitutionContractName;
        this.payerInstitutionContractCode = payerInstitutionContractCode;
        this.payerInstitutionContractBeginDate = payerInstitutionContractBeginDate;
        this.payerInstitutionContractEndDate = payerInstitutionContractEndDate;
        this.payerProviderContractCode = payerProviderContractCode;
        this.payerProviderContractName = payerProviderContractName;
        this.payerProviderContractBeginDate = payerProviderContractBeginDate;
        this.payerProviderContractEndDate = payerProviderContractEndDate;
        this.payerName = payerName;
        this.payerPhone = payerPhone;
        this.providerPhone = providerPhone;
        this.providerName = providerName;
        this.institutionPhone = institutionPhone;
        this.institutionName = institutionName;
        this.email = email;
        this.title = title;
        this.firstName = firstName;
        this.fatherName = fatherName;
        this.grandFatherName = grandFatherName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.insuredPhone = insuredPhone;
        this.branchOffice = branchOffice;
        this.position = position;
        this.idNumber = idNumber;
        this.insuranceId = insuranceId;
        this.profilePicture = profilePicture;
        this.premium = premium;
        this.remainingAmount = remainingAmount;
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.state = state;
        this.country = country;
        this.beginDate = beginDate;
        this.endDate = endDate;
        this.status = status;
    }
}