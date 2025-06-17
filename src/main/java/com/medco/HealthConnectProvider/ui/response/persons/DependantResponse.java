package com.medco.HealthConnectProvider.ui.response.persons;

import com.medco.HealthConnectProvider.utils.enums.Relationship;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

import java.util.Date;

@Data
public class DependantResponse {

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
    private String profilePictureBase64;


}
