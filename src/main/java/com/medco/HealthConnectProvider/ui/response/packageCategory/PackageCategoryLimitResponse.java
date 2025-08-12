package com.medco.HealthConnectProvider.ui.response.packageCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageCategoryLimitResponse {

    private String limitUuid;
    private String categoryUuid;
    private String categoryName;
    private String categoryCode;
    private String contractUuid;
    private String contractName;
    private String limitType;
    private BigDecimal limitValue;
    private String periodType;
    private LocalDate resetDate;
    private String description;
    private boolean isActive;
    private boolean isExpired;
    private BigDecimal totalUsed;
    private BigDecimal remainingLimit;
    private Instant createdAt;
    private Instant updatedAt;
}
