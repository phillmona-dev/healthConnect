package com.medco.HealthConnectProvider.ui.request.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.LocalDate;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateCbhiInsuredRequest {

    private String payerUuid;

    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String phone;
    private String email;
    private String nationalId;
    private String idNumber;
    private String insuranceId;
    private LocalDate birthDate;
    private String gender;
    private String address;
    private String city;
    private String state;
    private String country;

}