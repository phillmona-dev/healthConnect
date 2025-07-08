package com.medco.HealthConnectProvider.ui.response.persons;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InsuredDependantResponse {
    // Insured person properties
    private String insuredUuid;
    private String insuredTitle;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String gender;
    private String insuranceId;
    private String employeeId;
    private String phone;
    private String email;
    private Date birthDate;
    private Status status;
    private String address;
    private String woreda;
    private String kebelle;
    private String position;
    private String idNumber;

    // Dependants list
    private List<DependantInsuredResponse> dependants = new ArrayList<>();

    // Pagination metadata
    private long totalPages;
    private long totalElements;

    private String profilePicturePath;
    private String profilePictureBase64;
}
