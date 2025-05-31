package com.medco.HealthConnectProvider.ui.response.persons;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class InsuredResponse {

    private String insuredUuid;
    private String payerUuid;
    private String payerInstitutionContractUuid;
    private String email;

    private String title;

    private String firstName;


    private String fatherName;

    private String grandFatherName;


    private String Gender;


    private Date birthDate;


    private String phone;


    private String branchOffice;

    private String position;

    private String idNumber;

    private String insuranceId;

    double premium;

    private String address1;

    private String address2;

    private String address3;

    private String state;

    private String country;

    private Date beginDate;

    private Date endDate;

    private Status status;

    private long totalPages;

}
