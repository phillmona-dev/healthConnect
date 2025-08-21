package com.medco.HealthConnectProvider.dto.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalDispensingRequest {

    private Double totalPrice;
    private String providedDate;
    private String serviceProvidedUuid;
    private String insuredUuid;
    private String dependentUuid;
    private List<ExternalDispensingItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExternalDispensingItem {
        private String serviceId;
        private String serviceName;
        private String serviceCode;
        private Integer qty;
        private Double totalPrice;
        private String recordNumber;
        private String packageUuid;
    }
}
