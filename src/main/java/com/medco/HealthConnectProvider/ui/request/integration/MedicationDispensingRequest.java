package com.medco.HealthConnectProvider.ui.request.integration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String phoneNumber;
    
    @NotBlank(message = "Prescription number is required")
    private String prescriptionNumber;
    
    @NotBlank(message = "Pharmacy transaction ID is required")
    private String pharmacyTransactionId;
    
    @NotNull(message = "Dispensing date is required")
    private LocalDateTime dispensingDate;
    
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
        @NotBlank(message = "Medication code is required")
        private String medicationCode;
        
        @NotBlank(message = "Medication name is required")
        private String medicationName;
        
        @NotNull(message = "Quantity is required")
        private Double quantity;
        
        @NotBlank(message = "Unit of measure is required")
        private String unitOfMeasure;
        
        @NotNull(message = "Unit price is required")
        private Double unitPrice;
        
        @NotNull(message = "Total price is required")
        private Double totalPrice;
        
        private String dosageInstructions;
        private String strength;
        private String formulation; // tablet, capsule, syrup, etc.
    }
}