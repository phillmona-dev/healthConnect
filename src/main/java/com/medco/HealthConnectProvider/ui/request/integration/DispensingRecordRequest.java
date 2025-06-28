package com.medco.HealthConnectProvider.ui.request.integration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DispensingRecordRequest {

    private String insuredUuid;
    private String dependantUuid;
//    private String providerUuid;
//    private String payerUuid;
//    private String phone;
//    private String employeeId;
//    private LocalDate dispensingDate;
//    private String prescriptionNumber;
//    private String pharmacyTransactionId;
    private String primaryDiagnosis;
    private String secondaryDiagnosis;

    private List<DispensingItemRequest> medicationItems;

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DispensingItemRequest {
        private String serviceUuid;
        private String remark;
        private int quantity;
    }

}
