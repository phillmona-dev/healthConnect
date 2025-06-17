package com.medco.HealthConnectProvider.ui.request.integration;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DispensingRecordRequest {
    private String providerUuid;
    private String payerUuid;
    private String phone;
    private LocalDate dispensingDate;
    private String prescriptionNumber;
    private String pharmacyTransactionId;
    private List<DispensingItemRequest> medicationItems;

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DispensingItemRequest {
        private String serviceUuid;
        private Double quantity;
        private Double totalPrice;
        // Add other fields as needed
    }

}
