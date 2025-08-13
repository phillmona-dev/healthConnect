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
public class BulkServiceCategoryAssignmentResponse {

    private String categoryUuid;
    private String categoryName;
    private String categoryCode;
    private String contractUuid;
    private String contractName;
    private int totalServicesProcessed;
    private int successfulAssignments;
    private int failedAssignments;
    private int skippedAssignments; // Already existing mappings
    private List<ServiceAssignmentResult> results;
    private List<String> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceAssignmentResult {
        private String contractDetailUuid;
        private String serviceName;
        private String serviceCode;
        private String status; // SUCCESS, FAILED, SKIPPED, REPLACED
        private String message;
    }
}
