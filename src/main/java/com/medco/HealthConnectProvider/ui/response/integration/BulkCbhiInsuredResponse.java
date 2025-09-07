package com.medco.HealthConnectProvider.ui.response.integration;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BulkCbhiInsuredResponse {

    private String payerUuid;
    private String payerName;
    private int totalRequested;
    private int totalCreated;
    private int totalFailed;
    private LocalDateTime processedAt;
    private List<CreatedInsuredMember> createdMembers;
    private List<FailedInsuredMember> failedMembers;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreatedInsuredMember {
        private String insuredUuid;
        private String firstName;
        private String fatherName;
        private String grandFatherName;
        private String insuranceId;
        private String nationalId;
        private String phone;
        private String email;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FailedInsuredMember {
        private String firstName;
        private String fatherName;
        private String grandFatherName;
        private String insuranceId;
        private String nationalId;
        private String errorMessage;
        private String errorCode;
    }
}
