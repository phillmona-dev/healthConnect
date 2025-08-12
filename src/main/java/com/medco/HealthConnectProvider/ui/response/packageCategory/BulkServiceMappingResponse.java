package com.medco.HealthConnectProvider.ui.response.packageCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkServiceMappingResponse {

    private String contractUuid;
    private String contractName;
    private int totalServicesProcessed;
    private int successfulMappings;
    private int failedMappings;
    private List<MappingResult> results;
    private List<String> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MappingResult {
        private String serviceCode;
        private String serviceName;
        private String status; // SUCCESS, FAILED, SKIPPED
        private String message;
        private List<String> mappedCategories;
    }
}
