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
public class DispensingRecordRequest {

    private String insuredUuid;
    private String dependantUuid;
    private String primaryDiagnosis;
    private String secondaryDiagnosis;

    private List<DispensingItemRequest> medicationItems;

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DispensingItemRequest {
        private String contractDetailUuid;
        private String itemType; // "SERVICE" or "DRUG"
        private String remark;
        private double price;
        private int quantity;
    }

}
