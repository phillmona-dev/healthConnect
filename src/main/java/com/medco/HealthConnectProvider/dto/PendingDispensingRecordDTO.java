package com.medco.HealthConnectProvider.dto;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PendingDispensingRecordDTO {

    private String invoiceNumber;
    private String dispensingUuid;
    private String payerUuid;
    private String patientName;
    private String insuranceId;
    private LocalDate dispensingDate;
    private String prescriptionNumber;
    private String pharmacyTransactionId;
    private Double totalAmount;
    private Double patientResponsibility;
    private Double insuranceCoverage;
    private Status status;
    private String branchName;

    private LocalDate createdAt;
    private List<MedicationItemDTO> medicationItems;


    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class MedicationItemDTO {
        private String medicationName;
        private Double quantity;
        private String unitOfMeasure;
        private Double unitPrice;
        private Double totalPrice;

    }
}
