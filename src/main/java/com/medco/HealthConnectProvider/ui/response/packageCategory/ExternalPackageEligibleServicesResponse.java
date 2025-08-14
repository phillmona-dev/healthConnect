package com.medco.HealthConnectProvider.ui.response.packageCategory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalPackageEligibleServicesResponse {
    private String packageUuid;
    private String packageName;
    private String packageCategory;
    private String packageDescription;
    private List<Object> benefitRanges;
    private Double minLimit;
    private Double maxLimit;
    private String status;
    private String gender;
    private Integer totalPages;
    private List<EligibleServiceDto> packageEligibleServices;
}

