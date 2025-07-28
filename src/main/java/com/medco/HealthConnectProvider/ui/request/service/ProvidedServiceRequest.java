package com.medco.HealthConnectProvider.ui.request.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvidedServiceRequest {

    @NotBlank(message = "Contract detail UUID is required")
    private String contractDetailUuid;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Double quantity;
    
    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    private Double unitPrice;

}