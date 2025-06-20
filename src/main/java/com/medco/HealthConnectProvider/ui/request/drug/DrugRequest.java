package com.medco.HealthConnectProvider.ui.request.drug;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrugRequest {
    private String drugCode;
    private String drugName;
    private String category;
    private String subCategory;
    private BigDecimal price;
    private String dosage;
    private String manufacturer;
    private String status;

    private String genericName;
    private String brandName;
    private String formulation;
    private String route;
    private String indications;
    private String sideEffect;
    private String description;
}
