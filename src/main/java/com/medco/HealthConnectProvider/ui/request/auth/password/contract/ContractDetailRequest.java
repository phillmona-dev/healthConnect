package com.medco.HealthConnectProvider.ui.request.auth.password.contract;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractDetailRequest {
    @NotBlank(message = "Service UUID is required")
    private String serviceUuid;
    
    @NotNull(message = "Negotiated price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    private BigDecimal negotiatedPrice;
    
    private List<String> employeeGroupUuids;
}