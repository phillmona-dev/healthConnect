package com.medco.HealthConnectProvider.ui.request.auth.password.persons;

import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class InsuredRequest {

    @Size( max = 50)
    private String email;

    @Size(min = 36, max = 40)
    private String payerUuid;

//	@Size(min = 36, max = 40)
//	private String payerInstitutionContractUuid;

    double premium;


    @NotBlank(message = " title must be null ")
    @Size(min = 2, max = 25)
    private String title;


    @NotBlank(message = " firstName must be null ")
    @Size(min = 2, max = 25)
    private String firstName;


    @NotBlank(message = " fatherName must be null ")
    @Size(min = 2, max = 25)
    private String fatherName;


    @NotBlank(message = " grandFatherName must be null ")
    @Size(min = 2, max = 25)
    private String grandFatherName;


    @NotBlank(message = " Gender must be null ")
    @Size(min = 1, max = 10)
    private String Gender;

    @NotNull
    private Date birthDate;


    @NotBlank(message = " phone must be null ")
    @Size(min = 9, max = 13)
    private String phone;

    @Size( max = 50)
    private String branchOffice;


    @Size( max = 50)
    private String position;

    @Size( max = 50)
    private String idNumber;

    //
    @NotBlank(message = " insuranceId must be null ")
//	@Size(min = 1, max = 50)
//	private String insuranceId;


    @NotBlank(message = " address1 must be null ")
    @Size( max = 50)
    private String address1;


    @NotBlank(message = " address2 must be null ")
    @Size( max = 50)
    private String address2;


    @NotBlank(message = " address3 must be null ")
    @Size( max = 50)
    private String address3;


    @NotBlank(message = " state must be null ")
    @Size(min = 1, max = 50)
    private String state;


    @NotBlank(message = " country must be null ")
    @Size(min = 2, max = 50)
    private String country;

    private Status status;

}
