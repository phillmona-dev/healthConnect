package com.medco.HealthConnectProvider.ui.request.packageCategory;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibleServiceSearchRequest {

    // Basic search
    private String searchKey; // Search in service name, code, description
    
    // Category filtering
    @NotBlank(message = "Category name is required")
    private String categoryName; // Category name to filter by (required)
    private String categoryCode; // Category code to filter by
    private String categoryUuid; // Category UUID to filter by
    
    // Contract filtering
    @NotBlank(message = "Contract UUID is required")
    private String contractUuid; // Contract UUID (required)
    private String contractName; // Contract name filter
    
    // Service filtering
    private String serviceName;
    private String serviceCode;
    private String serviceCategory; // Service category (e.g., "Surgery", "Consultation")
    private String serviceSubCategory;
    private List<String> serviceCodes; // Multiple service codes
    
    // Price filtering
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String priceType; // "SERVICE_PRICE", "CONTRACT_PRICE"
    
    // Status filtering
    private String status; // "ACTIVE", "INACTIVE"
    private Boolean consumesFromLimit; // true/false/null (all)
    private Boolean isActive; // true/false/null (all)
    
    // Provider filtering
    private String providerUuid;
    private String providerName;
    
    // Sorting
    private String sortBy; // "serviceName", "serviceCode", "price", "mappedAt"
    private String sortDirection; // "ASC", "DESC"
    
    // Pagination
    @Builder.Default
    private int page = 0;
    
    @Builder.Default
    private int size = 20;
    
    // Advanced filters
    private Boolean hasMultipleCategories; // Services mapped to multiple categories
    private List<String> excludeServiceCodes; // Exclude specific service codes
    private String dateRange; // "TODAY", "WEEK", "MONTH", "YEAR" for mapping date
}
