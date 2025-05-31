package com.medco.HealthConnectProvider.ui.response.persons;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.Data;

@Data
public class InsuredDependantResponse {
    // Insured person properties
    private String insuredPersonUuid;
    private String insuredTitle;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String gender;
    private String insuranceId;
    private String phone;
    private String email;
    private Date birthDate;
    private Status status;
    private String address1;
    private String address2;
    private String address3;

    // Dependants list
    private List<DependantInsuredResponse> dependants = new ArrayList<>();

    // Pagination metadata
    private long totalPages;
    private long totalElements;
}
