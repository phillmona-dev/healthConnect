package com.medco.HealthConnectProvider.ui.response.persons;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class InsuredWithDependantsResponse {
    private String insuredUuid;
    private String payerUuid;
    private String employeeId;
    private String nationalId;
    private String policyNumber;
    private LocalDate policyStartDate;
    private LocalDate policyEndDate;
    private String email;
    private String title;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String gender;
    private Date birthDate;
    private String phone;
    private String branchOffice;
    private String position;
    private boolean dependantCoverage;
    private String idNumber;
    private String insuranceId;
    private String address;
    private String state;
    private String country;
    private String profilePictureBase64;
    private Status status;
    private List<DependantResponse> dependants;

}
