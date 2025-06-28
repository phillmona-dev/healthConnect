package com.medco.HealthConnectProvider.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationDispensingDTO {

    private String dispensingUuid;
    private String invoiceNumber;
    private String batchCode;
    private String providerUuid;
    private String providerName;
    private String payerUuid;
    private String payerName;
    private String insuredUuid;
    private String insuredName;
    private String insuranceId;
    private String prescriptionNumber;
    private String pharmacyTransactionId;
    private LocalDate dispensingDate;
    private String prescribingPhysicianName;
    private String prescribingPhysicianId;
    private LocalDate recordedAt;
    private String branchName;
    private String claimStatus;
    private String claimUuid;
    private Double totalAmount;
    private Double patientResponsibility;
    private Double insuranceCoverage;
    private String pharmacistNotes;
    private List<MedicationItemDTO> medicationItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationItemDTO {

        private String itemUuid;
        private String medicationCode;
        private String medicationName;
        private Double quantity;
        private String primaryDiagnosis;
        private String secondaryDiagnosis;
        private String unitOfMeasure;
        private Double unitPrice;
        private Double totalPrice;
        private String dosageInstructions;
        private String strength;
        private String route;
        private String formulation;
        private String itemType;

    }

}
