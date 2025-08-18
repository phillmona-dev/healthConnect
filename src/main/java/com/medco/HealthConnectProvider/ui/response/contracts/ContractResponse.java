package com.medco.HealthConnectProvider.ui.response.contracts;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
    private String rejectionReason;
    private String preparedBy;
    private String contractCode;
    private Double coPaymentPercentage;
    private String description;
    private boolean isDeleted;

    private String payerUuid;
    private String payerName;
    private String payerCode;
    private String payerTelephone;
    private String payerAddress;
    private String payerContactEmail;

    private String providerUuid;
    private String providerName;
    private String providerCode;
    private String providerTelephone;
    private String providerAddress;
    private String providerContactEmail;

    private Date terminationDate;
    private String terminationReason;
    private String terminationNotes;
    private String terminatedBy;
    private Date terminationRequestDate;

    private Instant createdAt;
    private Instant updatedAt;

    private int totalServices;
    private int totalInsured;
    private int totalDependants;
    private int totalDrugs;

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
        private String serviceCode;
        private String drugUuid;
        private String drugName;
        private Double price;
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
        private String phone;
        private List<DependantSummary> dependants;

        public void filterDependants(String searchKey) {
            if (searchKey != null && !searchKey.isEmpty()) {
                this.dependants = this.dependants.stream()
                        .filter(dep -> dep.getFullName().toLowerCase().contains(searchKey.toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
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
        private String phone;

    }

    private String payerLogoBase64;
    private String providerLogoBase64;

}
