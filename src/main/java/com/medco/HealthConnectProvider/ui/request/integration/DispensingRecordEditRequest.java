package com.medco.HealthConnectProvider.ui.request.integration;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DispensingRecordEditRequest {
    private LocalDate dispensingDate;
    private String prescriptionNumber;
    private String pharmacyTransactionId;
    private String pharmacistNotes;
    private String primaryDiagnosis;
    private String secondaryDiagnosis;
    private List<DispensingItemEditRequest> medicationItems;

    @Data
    public static class DispensingItemEditRequest {
        private String serviceUuid;
        private Double quantity;
        private String remark;
    }
}
