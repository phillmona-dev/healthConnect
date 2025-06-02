package com.medco.HealthConnectProvider.ui.request.eligibility;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityCheckRequest {
    
    @NotBlank(message = "Payer UUID is required")
    private String payerUuid;
    
    // One of the following identifiers must be provided
    private String employeeId;
    private String insuranceId;
    private String nationalId;
    private String phoneNumber;
    
    // Optional - specific service to check eligibility for
    private String serviceUuid;
}