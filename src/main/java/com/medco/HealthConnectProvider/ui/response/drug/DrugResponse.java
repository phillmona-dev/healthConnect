package com.medco.HealthConnectProvider.ui.response.drug;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrugResponse {
    private String drugUuid;
    private String drugCode;
    private String drugName;
    private String category;
    private String subCategory;
    private BigDecimal price;
    private String dosage;
    private String manufacturer;
    private Status status;
    private String providerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int totalPages;
}
