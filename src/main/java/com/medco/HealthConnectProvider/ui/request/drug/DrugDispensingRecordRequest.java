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
public class DrugDispensingRecordRequest {

    private String dependantUuid;
    private String providerUuid;
    private String payerUuid;
    private String phone;
    private String employeeId;
    private LocalDate dispensingDate;
    private String prescriptionNumber;
    private String pharmacyTransactionId;
    private List<DrugDispensingItemRequest> drugItems;

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DrugDispensingItemRequest {
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
