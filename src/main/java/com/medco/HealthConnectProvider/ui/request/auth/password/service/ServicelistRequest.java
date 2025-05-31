package com.medco.HealthConnectProvider.ui.request.auth.password.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServicelistRequest {

    @Size( max = 30)
    private String serviceCode;
    @NotBlank
    @Size( max = 500)
    private String serviceName;

    @Size( max = 150)
    private String subCategory;

    @Size( max = 150)
    private String category;

    double price;

    @NotBlank
    @Size(min = 3, max = 25)
    private String status;

}