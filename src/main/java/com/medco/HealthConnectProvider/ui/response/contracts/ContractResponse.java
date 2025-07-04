package com.medco.HealthConnectProvider.ui.response.contracts;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractResponse {
    private String contractHeaderUuid;
    private String contractNumber;
    private String contractName;
    private String contractDescription;
    private String approvedBy;
    private Date approvalDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private Status status;
    private String remark;
    private String preparedBy;
    private String contractCode;
    private Double coPaymentPercentage;
    private String description;
    private boolean isDeleted;

    // Payer information
    private String payerUuid;
    private String payerName;
    private String payerCode;

    // Provider information
    private String providerUuid;
    private String providerName;
    private String providerCode;

    // Termination information
    private Date terminationDate;
    private String terminationReason;
    private String terminationNotes;
    private String terminatedBy;
    private Date terminationRequestDate;

    // Audit information
    private Instant createdAt;
    private Instant updatedAt;

    // Summary information
    private int totalServices;
    private int totalInsured;
    private int totalDependants;
    private int totalDrugs;

    // You might want to include a list of contract details or a summary of them
    private List<ContractDetailSummary> contractDetails;
    private List<InsuredSummary> insuredSummaries;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContractDetailSummary {
        private String contractDetailUuid;
        private String itemType;
        private String serviceUuid;
        private String serviceName;
        private String drugUuid;
        private String drugName;
        private BigDecimal price;
        private Double negotiatedPrice;
        private List<String> assignedGroups;
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InsuredSummary {
        private String insuredUuid;
        private String fullName;
        private String membershipNumber;
        private List<DependantSummary> dependants;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DependantSummary {
        private String dependantUuid;
        private String fullName;
        private String relationshipType;
    }


    private String payerLogoBase64;
    private String providerLogoBase64;
}
