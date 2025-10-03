package com.medco.HealthConnectProvider.dto.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalClaimRejectionResponse {

    private String batchCode;
    private String claimUuid;
    private String contractUuid;
    private String providerUuid;
    private String status; // "REJECTED", "PARTIALLY_REJECTED"
    private String message;
    private LocalDateTime processedAt;
    private Double rejectedAmount;
    private List<DispensingRejectionDetail> rejectedDispensing;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DispensingRejectionDetail {
        private String dispensingUuid;
        private String rejectionCode;
        private String rejectionReason;
        private String rejectionCategory; // "ELIGIBILITY", "COVERAGE", "DOCUMENTATION", "DUPLICATE", "FRAUD"
        private Double rejectedAmount;
        private LocalDateTime rejectedAt;
        private String reviewerComments;
        private Boolean canResubmit;
        private List<String> requiredDocuments;
    }
}
