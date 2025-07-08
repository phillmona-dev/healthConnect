package com.medco.HealthConnectProvider.ui.response.persons;

import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
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

    private String employeeId;

    private String nationalId;

    private Date birthDate;

    private String phone;

    private String branchOffice;

    private String position;

    private String idNumber;

    private String insuranceId;

    double premium;

    private String address;

    private String state;
    private String woreda;
    private String kebelle;
    private String country;

    private Status status;

    private long totalPages;

    private String photoBase64;

    private String profilePicturePath;

}
