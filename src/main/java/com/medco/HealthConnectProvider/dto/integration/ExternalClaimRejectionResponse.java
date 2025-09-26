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
    private String status; // "APPROVED", "REJECTED", "PARTIALLY_APPROVED"
    private String message;
    private LocalDateTime processedAt;
    private Double approvedAmount;
    private Double rejectedAmount;
    private List<DispensingRejectionDetail> rejectedDispensing;
    private List<DispensingApprovalDetail> approvedDispensing;

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
        private boolean canResubmit;
        private List<String> requiredDocuments; // Documents needed for resubmission
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DispensingApprovalDetail {
        private String dispensingUuid;
        private String approvalCode;
        private Double approvedAmount;
        private Double originalAmount;
        private LocalDateTime approvedAt;
        private String reviewerComments;
        private String paymentReference;
    }
}
