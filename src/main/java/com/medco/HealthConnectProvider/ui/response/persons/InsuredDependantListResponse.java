package com.medco.HealthConnectProvider.ui.response.persons;

import com.medco.HealthConnectProvider.utils.enums.Relationship;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InsuredDependantListResponse {
    private String insuredUuid;
    private String firstName;
    private String insuranceId;
    private String fatherName;
    private String email;
    private String phone;
    private String grandFatherName;
    private String title;
    private String gender;
    private String dependantFirstName;
    private String dependantFatherName;
    private String dependantGrandFatherName;
    private String dependantGender;
    private Relationship relationship;
    private String dependantUuid;
    private Status dependantStatus;

    // Explicit constructor for JPQL query
    public InsuredDependantListResponse(
            String insuredUuid, String firstName, String insuranceId,
            String fatherName, String email, String phone, String grandFatherName,
            String title, String gender, String dependantFirstName,
            String dependantFatherName, String dependantGrandFatherName,
            String dependantGender, Relationship relationship, String dependantUuid,
            Status dependantStatus) {
        this.insuredUuid = insuredUuid;
        this.firstName = firstName;
        this.insuranceId = insuranceId;
        this.fatherName = fatherName;
        this.email = email;
        this.phone = phone;
        this.grandFatherName = grandFatherName;
        this.title = title;
        this.gender = gender;
        this.dependantFirstName = dependantFirstName;
        this.dependantFatherName = dependantFatherName;
        this.dependantGrandFatherName = dependantGrandFatherName;
        this.dependantGender = dependantGender;
        this.relationship = relationship;
        this.dependantUuid = dependantUuid;
        this.dependantStatus = dependantStatus;
    }
}

