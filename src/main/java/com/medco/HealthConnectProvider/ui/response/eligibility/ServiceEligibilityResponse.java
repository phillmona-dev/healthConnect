package com.medco.HealthConnectProvider.ui.response.eligibility;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceEligibilityResponse {
    private String serviceUuid;
    private String serviceCode;
    private String serviceName;
    private String category;
    private String subCategory;
    
    private boolean isCovered;
    private BigDecimal price;
    private BigDecimal coPaymentAmount;
    private Double coPaymentPercentage;
    private BigDecimal insuranceCoverage;
    
    // Group-specific pricing if applicable
    private String appliedGroupUuid;
    private String appliedGroupName;
}