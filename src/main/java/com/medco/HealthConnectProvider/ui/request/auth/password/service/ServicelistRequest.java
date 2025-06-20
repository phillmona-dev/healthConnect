package com.medco.HealthConnectProvider.ui.request.auth.password.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServicelistRequest {

    @NotBlank(message = "Service code is required")
    private String serviceCode;

    @NotBlank(message = "Service name is required")
    private String serviceName;

    private String subCategory;

    private String serviceCategory;

    @NotNull(message = "Price is required")
    private Double price;

    private String status;

    private String serviceDescription;
}