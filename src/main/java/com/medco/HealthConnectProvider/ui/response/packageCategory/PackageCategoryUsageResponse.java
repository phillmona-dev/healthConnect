package com.medco.HealthConnectProvider.ui.response.packageCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageCategoryUsageResponse {

    private String usageUuid;
    private String insuredPersonUuid;
    private String insuredPersonName;
    private String categoryUuid;
    private String categoryName;
    private String categoryCode;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private BigDecimal usedAmount;
    private Double usedQuantity;
    private Integer usedVisits;
    private LocalDateTime serviceDate;
    private String serviceUuid;
    private String serviceName;
    private String claimUuid;
    private String providedServiceUuid;
    private String notes;
    private BigDecimal remainingAmount;
    private LocalDateTime createdAt;
}
