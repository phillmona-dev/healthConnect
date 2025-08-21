package com.medco.HealthConnectProvider.ui.response.integration;

import com.medco.HealthConnectProvider.entity.integration.MedicationDispensing;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispensingRecordResponse {
    private String invoiceNumber;
    private String dispensingUuid;
    private String status;
    private String message;
    private LocalDate recordedAt;
    private Double totalAmount;
    private Double patientResponsibility;
    private Double insuranceCoverage;

    // Optional attachment details (if an attachment was provided on request)
    private String attachmentFileName;
    private String attachmentContentType;
    private String attachmentBase64; // raw base64 string (no data: prefix)

    public DispensingRecordResponse(MedicationDispensing savedRecord) {
        this.invoiceNumber = savedRecord.getInvoiceNumber();
        this.dispensingUuid = savedRecord.getDispensingUuid();
        this.status = "SUCCESS";
        this.message = "Dispensing record created successfully";
        this.recordedAt = savedRecord.getRecordedAt();
        this.totalAmount = savedRecord.getTotalAmount();
        this.patientResponsibility = savedRecord.getPatientResponsibility();
        this.insuranceCoverage = savedRecord.getInsuranceCoverage();
    }
}
