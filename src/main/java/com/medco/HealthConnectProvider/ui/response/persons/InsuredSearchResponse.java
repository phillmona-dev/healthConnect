package com.medco.HealthConnectProvider.ui.response.persons;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InsuredSearchResponse {
    private String insuredUuid;
    private String firstName;
    private String fatherName;
    private String payerUuid;
    private String grandFatherName;
    private String phone;
    private String employeeId;
    private String insuranceId;
    private String nationalId;
    private String payerName;
    private Status status;
    private Date birthDate;
    private String profilePictureBase64;


    private boolean isInsured;
    private List<DependantResponse> dependants;
}
