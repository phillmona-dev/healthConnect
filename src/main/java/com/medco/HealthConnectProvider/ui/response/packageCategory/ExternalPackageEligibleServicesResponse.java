package com.medco.HealthConnectProvider.ui.response.packageCategory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.medco.HealthConnectProvider.utils.enums.PackageGender;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    private Status status;

    private Double sumAssured;

    private Double usage;

    @Enumerated(EnumType.STRING)
    private PackageGender gender;

    private Double excessAllowedAmount;
    private boolean isExcessAllowed;

    private List<EligibleServiceDto> packageEligibleServices;
}

