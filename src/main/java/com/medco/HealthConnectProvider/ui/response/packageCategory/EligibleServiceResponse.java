package com.medco.HealthConnectProvider.ui.response.packageCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibleServiceResponse {

    private String contractDetailUuid;
    private String serviceUuid;
    private String serviceName;
    private String serviceCode;
    private String serviceId;
    private String serviceDescription;
    private String serviceCategory;
    private String serviceSubCategory;
    private Double servicePrice;
    private Double contractPrice;
    private String priceType;
    private boolean consumesFromLimit;
    private String mappingNotes;
    private Instant mappedAt;
    private String mappedBy;
    
    // Contract information
    private String contractUuid;
    private String contractName;
    
    // Category information
    private String categoryUuid;
    private String categoryName;
    private String categoryCode;
    
    // Provider information
    private String providerUuid;
    private String providerName;
    
    // Additional service details
    private String status;
    private boolean isActive;
    private List<String> additionalCategories; // Other categories this service is mapped to
}
