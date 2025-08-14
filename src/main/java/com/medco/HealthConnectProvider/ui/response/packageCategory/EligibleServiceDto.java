package com.medco.HealthConnectProvider.ui.response.packageCategory;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EligibleServiceDto {
    private String eligibleServiceUuid;
    private String itemCode;
    private String item;
    private String subCategory;
    private String category;
    private Double price;
}
