package com.medco.HealthConnectProvider.ui.response.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ServicelistResponse implements Serializable {
    private String serviceUuid;
    private String serviceCode;
    private String serviceName;
    private String serviceSubCategory;
    private String serviceCategory;
    private BigDecimal price;
    private String status;
    private int totalPages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String providerName;
}