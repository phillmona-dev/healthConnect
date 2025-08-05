package com.medco.HealthConnectProvider.ui.request.integration;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DispensingRecordEditRequest {
    private String claimUuid;
    private String insuredUuid;
    private String dependantUuid;
    private String primaryDiagnosis;
    private String secondaryDiagnosis;
    private String claimStatus;
    private List<DispensingItemEditRequest> medicationItems;

    @Data
    public static class DispensingItemEditRequest {
        private String itemUuid;
        private String contractDetailUuid;
        private String itemType;
        private String remark;
        private double price;
        private int quantity;
    }
}
