package com.medco.HealthConnectProvider.ui.request.auth.password.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractTerminationRequest {
    @NotNull(message = "Termination date is required")
    private LocalDate terminationDate;
    
    @NotBlank(message = "Termination reason is required")
    private String terminationReason;
    
    private String additionalNotes;
}