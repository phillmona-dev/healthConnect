package com.medco.HealthConnectProvider.ui.response.service;

import com.medco.HealthConnectProvider.utils.enums.ServiceCategory;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {
    
    private String serviceUuid;
    private String serviceName;
    private String serviceCode;
    private String description;
    private Double standardPrice;
    private ServiceCategory category;
    private Status status;
    
    // Provider information
    private String providerUuid;
    private String providerName;
    
    // Audit information
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}