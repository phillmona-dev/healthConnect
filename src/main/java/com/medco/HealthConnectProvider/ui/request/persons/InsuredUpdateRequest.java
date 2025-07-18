package com.medco.HealthConnectProvider.ui.request.persons;

import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class InsuredUpdateRequest {

    private String title;

    @NotBlank(message = " firstName must not be null ")
    private String firstName;

    @NotBlank(message = " fatherName must not be null ")
    private String fatherName;

    private String grandFatherName;
    private String gender;
    private Date birthDate;
    private String idNumber;

    @Size(min = 9, max = 13, message = "Phone number must be between 9 and 13 characters")
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

    double premium;

    @Size(max = 50)
    private String employeeId;

    @Size(max = 50)
    private String nationalId;

    private String Gender;

    private String woreda;

    private String city;

    private String subcity;

    private String groupUuid;

    private Date inactiveDate;

}
