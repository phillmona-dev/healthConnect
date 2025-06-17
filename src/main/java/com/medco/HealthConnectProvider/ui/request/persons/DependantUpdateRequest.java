package com.medco.HealthConnectProvider.ui.request.persons;

import com.medco.HealthConnectProvider.utils.enums.Relationship;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DependantUpdateRequest {
    private String title;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String gender;
    private Date birthDate;
    private Relationship relationship;
    private Status status;
}