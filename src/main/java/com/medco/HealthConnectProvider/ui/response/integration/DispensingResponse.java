package com.medco.HealthConnectProvider.ui.response.integration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispensingResponse {
    private String dispensingUuid;
    private String status;
    private String message;
    private LocalDateTime recordedAt;
    private Double totalAmount;
    private Double patientResponsibility;
    private Double insuranceCoverage;
}