package com.medco.HealthConnectProvider.ui.request.drug;

import com.medco.HealthConnectProvider.utils.enums.ItemType;
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
public class DrugDispensingRecordEditRequest {
    private String dispensingUuid;
    private String providerUuid;
    private String payerUuid;
    private String insuredUuid;
    private String dependantUuid;
    private LocalDate dispensingDate;
    private String prescriptionNumber;
    private String pharmacyTransactionId;
    private String prescribingPhysicianName;
    private String prescribingPhysicianId;
    private String branchName;
    private String pharmacistNotes;
    private String primaryDiagnosis;
    private String secondaryDiagnosis;
    private List<DrugDispensingItemEditRequest> medicationItems;

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DrugDispensingItemEditRequest {
        private String itemUuid;
        private String drugUuid;
        private Double quantity;
        private Double totalPrice;
        private String route;
        private String frequency;
        private String dose;
        private String duration;
        private ItemType itemType = ItemType.DRUG;
    }
}
