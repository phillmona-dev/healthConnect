package com.medco.HealthConnectProvider.services.persons;

import java.time.LocalDate;
import java.util.Date;

public interface InsuredEligibilityProjection {
    String getInsuredUuid();
    String getPayerInstitutionContractName();
    String getPayerInstitutionContractCode();
    LocalDate getPayerInstitutionContractBeginDate();
    LocalDate getPayerInstitutionContractEndDate();
    String getPayerProviderContractCode();
    String getPayerProviderContractName();
    LocalDate getPayerProviderContractBeginDate();
    LocalDate getPayerProviderContractEndDate();
    String getPayerName();
    String getPayerPhone();
    String getProviderPhone();
    String getProviderName();
    String getInstitutionPhone();
    String getInstitutionName();
    String getEmail();
    String getTitle();
    String getFirstName();
    String getFatherName();
    String getGrandFatherName();
    String getGender();
    Date getBirthDate();
    String getInsuredPhone();
    String getBranchOffice();
    String getPosition();
    String getIdNumber();
    String getInsuranceId();
    String getProfilePicture();
    double getPremium();
    double getRemainingAmount();
    String getAddress();
    String getState();
    String getCountry();
    String getStatus();
}
