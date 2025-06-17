package com.medco.HealthConnectProvider.ui.request.integration;

import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationDispensingRequest {

    @NotBlank(message = "Provider UUID is required")
    private String providerUuid;
    
    @NotBlank(message = "Payer UUID is required")
    private String payerUuid;
    
    // Patient identification - at least one must be provided
    private String employeeId;
    private String insuranceId;
    private String nationalId;
    private String phone;

    private Status status = Status.PENDING;

    private String branchName;
    
    @NotBlank(message = "Prescription number is required")
    private String prescriptionNumber;
    
    @NotBlank(message = "Pharmacy transaction ID is required")
    private String pharmacyTransactionId;
    
    @NotNull(message = "Dispensing date is required")
    private LocalDate dispensingDate;
    
    private String prescribingPhysicianName;
    private String prescribingPhysicianId;
    
    @NotEmpty(message = "At least one medication item is required")
    @Valid
    private List<MedicationItem> medicationItems;
    
    private String pharmacistNotes;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationItem {

        private String medicationCode;

        private String medicationName;

        private Double quantity;

        private String unitOfMeasure;

        private Double unitPrice;

        private Double totalPrice;
        
        private String dosageInstructions;
        private String strength;
        private String formulation;

        private String serviceUuid;

    }
}