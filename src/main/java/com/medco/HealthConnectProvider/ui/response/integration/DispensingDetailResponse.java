package com.medco.HealthConnectProvider.ui.response.integration;

import com.medco.HealthConnectProvider.utils.enums.SourceType;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DispensingDetailResponse {
    private String dispensingUuid;
    private String invoiceNumber;
    private String batchCode;
    private String providerUuid;
    private String payerUuid;
    private String insuredUuid;
    private String prescriptionNumber;
    private String pharmacyTransactionId;
    private LocalDate dispensingDate;
    private String prescribingPhysicianName;
    private String prescribingPhysicianId;
    private LocalDate recordedAt;
    private String branchName;
    private String claimStatus;
    private Status status;
    private String claimUuid;
    private Double totalAmount;
    private Double patientResponsibility;
    private Double insuranceCoverage;
    private String pharmacistNotes;
    private SourceType source;
    private String primaryDiagnosis;
    private String secondaryDiagnosis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DispensingItemDetail> items;

    @Data
    public static class DispensingItemDetail {
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
        private String remark;
    }
}