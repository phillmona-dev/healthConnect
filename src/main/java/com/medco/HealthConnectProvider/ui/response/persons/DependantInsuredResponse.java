package com.medco.HealthConnectProvider.ui.response.persons;

import com.medco.HealthConnectProvider.utils.enums.Relationship;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class DependantInsuredResponse {
    // Reference to parent insured person
    private String insuredPersonUuid;
    private String dependantUuid;
    private String dependantFirstName;
    private String dependantFatherName;
    private String dependantGrandFatherName;
    private String dependantGender;
    private Status dependantStatus;
    private Relationship relationship;
    private Date dependantBirthDate;

}
