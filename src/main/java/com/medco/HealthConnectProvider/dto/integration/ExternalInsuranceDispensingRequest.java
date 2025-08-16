package com.medco.HealthConnectProvider.dto.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalInsuranceDispensingRequest {

    private List<DispensingItemPayload> dispensingItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DispensingItemPayload {
        private String serviceId;
        private String insuredUuid;
        private String insuredType; // "INSURED" or "DEPENDANT"
        private Integer quantity;
        private BigDecimal totalPrice;
        private String itemType; // "SERVICE" or "DRUG"
        private String packageUuid;
        private String providerUuid;
        private LocalDate dispensingDate;
        private String dispensingUuid;
    }
}
