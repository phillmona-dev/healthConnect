package com.medco.HealthConnectProvider.ui.response.packageCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryLimitSummaryResponse {

    private String insuredPersonUuid;
    private String insuredPersonName;
    private String contractUuid;
    private String contractName;
    private List<CategoryLimitDetail> categoryLimits;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryLimitDetail {
        private String categoryUuid;
        private String categoryName;
        private String categoryCode;
        private String limitType;
        private BigDecimal limitValue;
        private BigDecimal usedAmount;
        private BigDecimal remainingAmount;
        private Double usedQuantity;
        private Integer usedVisits;
        private String periodType;
        private LocalDate resetDate;
        private boolean isExpired;
        private double utilizationPercentage;
    }
}
