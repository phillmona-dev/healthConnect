package com.medco.HealthConnectProvider.ui.request.auth.password.payer;

import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PayerRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String payerName;

    private Long tinNumber;

    @NotBlank
    @Size(min = 5, max = 50)
    @Email
    private String email;


    @Size( max = 500)
    private String description;

    @NotBlank
    @Size(min = 9, max = 13)
    private String telephone;


    @Size( max = 50)
    private String category;

    @NotBlank
    @Size(max = 50)
    private String address1;

    @NotBlank
    @Size(max = 50)
    private String state;

    @NotBlank
    @Size(max = 50)
    private String country;

    private double latitude;
    private double longitude;

    @Size(max = 15)
    private String referralType;


    @Size(max = 100)
    private String referredBy;

    private Status status;
}
