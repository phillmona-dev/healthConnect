package com.medco.HealthConnectProvider.ui.request.auth.password.payer;

import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PayerRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String payerName;

    @Pattern(regexp = "^\\d{10,13}$", message = "TIN number must be between 10 and 13 digits")
    private String tinNumber;

    private String email;

    @Size( max = 500)
    private String description;

    @Size(min = 9, max = 13)
    private String telephone;

    @Size( max = 50)
    private String category;

    @Size(max = 50)
    private String address1;

    private String address2;

    private String address3;

    @Size(max = 50)
    private String state;

    @Size(max = 50)
    private String country;

    private double latitude;
    private double longitude;

    @Size(max = 15)
    private String referralType;

    @Size(max = 100)
    private String referredBy;

    private Status status;

    private boolean dependantCoverage;

    private boolean isCbhi;

}
