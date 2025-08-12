package com.medco.HealthConnectProvider.ui.response.packageCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageCategoryResponse {

    private String categoryUuid;
    private String categoryName;
    private String categoryCode;
    private String description;
    private String status;
    private String payerName;
    private String payerUuid;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private long totalServices;
    private long totalContracts;
}
