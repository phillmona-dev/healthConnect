package com.medco.HealthConnectProvider.ui.response.packageCategory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalPackageCategoryResponse {
    private String packageUuid;
    private String packageName;
    private double sumAssured;
}
