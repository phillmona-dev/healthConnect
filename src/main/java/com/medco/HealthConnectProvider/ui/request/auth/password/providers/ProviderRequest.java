package com.medco.HealthConnectProvider.ui.request.auth.password.providers;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProviderRequest {

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(unique = true)
    private String threeDigitAcronym;

    @NotBlank
    @Size(min = 3, max = 50)
    private String providerName;

    @Size(max = 100)
    private String description;

    @Size(min = 5, max = 50)
    @Email
    private String email;

    @Size(min = 9, max = 13)
    private String telephone;

    @Size(min = 3, max = 50)
    private String category;

    @Size(min = 3, max = 50)
    private String level;

    @Size(min = 1, max = 50)
    private String address1;

    @Size(min = 2, max = 50)
    private String state;

    @Size(min = 2, max = 50)
    private String country;

    private double latitude;

    private double longitude;

    @Pattern(regexp = "^\\d{10,13}$", message = "TIN number must be between 10 and 13 digits")
    private String tinNumber;

    @Size(min = 3, max = 15)
    private String status;

    private String branch;

}