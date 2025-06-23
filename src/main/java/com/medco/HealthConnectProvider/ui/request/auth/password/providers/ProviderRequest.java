package com.medco.HealthConnectProvider.ui.request.auth.password.providers;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProviderRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String providerName;

    @Size(min = 3, max = 100)
    private String description;
    @NotBlank
    @Size(min = 5, max = 50)
    @Email
    private String email;

    @NotBlank
    @Size(min = 9, max = 13)
    private String telephone;

    @NotBlank
    @Size(min = 3, max = 50)
    private String category;

    @NotBlank
    @Size(min = 3, max = 50)
    private String level;

    @NotBlank
    @Size(min = 1, max = 50)
    private String address1;

    @NotBlank
    @Size(min = 2, max = 50)
    private String state;

    @NotBlank
    @Size(min = 2, max = 50)
    private String country;

    private double latitude;

    private double longitude;
    @NotBlank
    @Size(min = 3, max = 15)
    private String tinNumber;

    @NotBlank
    @Size(min = 3, max = 15)
    private String status;

    private String branch;

}