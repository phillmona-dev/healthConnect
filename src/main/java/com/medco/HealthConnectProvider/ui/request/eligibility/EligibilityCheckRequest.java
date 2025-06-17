package com.medco.HealthConnectProvider.ui.request.eligibility;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityCheckRequest {
    
    // One of the following identifiers must be provided
    private String employeeId;
    private String insuranceId;
    private String nationalId;
    private String phoneNumber;
    
    // Optional - specific service to check eligibility for
    private String serviceUuid;

    public void setServiceUuid(String serviceUuid) {
        if (serviceUuid != null && !serviceUuid.isEmpty() && !serviceUuid.equals("string")) {
            try {
                UUID.fromString(serviceUuid);
                this.serviceUuid = serviceUuid;
            } catch (IllegalArgumentException e) {
                // If it's not a valid UUID, set it to null
                this.serviceUuid = null;
            }
        } else {
            this.serviceUuid = null;
        }
    }
}