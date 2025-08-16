package com.medco.HealthConnectProvider.ui.request.packageCategory;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    private String searchKey;

    @NotBlank(message = "Category name is required")
    private String categoryName;
    private String categoryCode;
    private String categoryUuid;

    @NotBlank(message = "Contract UUID is required")
    private String contractUuid;
    private String contractName;

    private String serviceName;
    private String serviceCode;
    private String serviceCategory;
    private String serviceSubCategory;
    private List<String> serviceCodes;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String priceType; // "SERVICE_PRICE", "CONTRACT_PRICE"

    private String status;
    private Boolean consumesFromLimit;
    private Boolean isActive;

    private String providerUuid;
    private String providerName;

    private String sortBy; // "serviceName", "serviceCode", "price", "mappedAt"
    private String sortDirection;

    @Builder.Default
    @Min(value = 1, message = "Page number must be at least 1")
    private int page = 1;

    @Builder.Default
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private int size = 25;

    private Boolean hasMultipleCategories;
    private List<String> excludeServiceCodes;
    private String dateRange; // "TODAY", "WEEK", "MONTH", "YEAR" for mapping date

    public int getPageForQuery() {
        return page - 1;
    }

}