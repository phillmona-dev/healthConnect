package com.medco.HealthConnectProvider.ui.request.integration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DispensingRecordEditRequest {

    // Fields matching DispensingRecordRequest
    private String contractHeaderUuid;
    private String insuredUuid;
    private String dependantUuid;
    private String primaryDiagnosis;
    private String secondaryDiagnosis;

    private Boolean isInsurance;
    private String packageUuid;
    private String dispensingDate;

    private List<DispensingItemEditRequest> medicationItems;

    // Additional fields specific to edit operation
    private String claimUuid;
    private String claimStatus;

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DispensingItemEditRequest {
        private String itemUuid; // For identifying existing items during edit
        private String contractDetailUuid;
        private String serviceId;
        private String itemType; // "SERVICE" or "DRUG"
        private String remark;
        private double price;
        private int quantity;
    }
}
