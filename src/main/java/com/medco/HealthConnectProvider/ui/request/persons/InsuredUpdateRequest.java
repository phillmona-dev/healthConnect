package com.medco.HealthConnectProvider.ui.request.persons;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class InsuredUpdateRequest {
    private String title;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String gender;
    private Date birthDate;
    private String idNumber;
    private String phone;
    private String email;
    private String branchOffice;
    private String position;
    private String address;
    private String state;
    private String country;
    private String insuranceId;
    private Status status;
    private String payerUuid;
}
