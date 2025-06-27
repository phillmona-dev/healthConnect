package com.medco.HealthConnectProvider.ui.response.contracts;

import com.medco.HealthConnectProvider.utils.enums.Relationship;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DetailedContractResponse {

    private String contractHeaderUuid;
    private String contractNumber;
    private String contractName;
    private String contractDescription;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String payerName;
    private String providerName;
    private Double coPaymentPercentage;
    private List<ContractDetailResponse> contractDetails;
    private List<InsuredResponse> insured;

    @Data
    public static class ContractDetailResponse {
        private String contractDetailUuid;
        private String serviceUuid;
        private String serviceName;
        private Double negotiatedPrice;
        private List<String> employeeDependantGroups;
    }

    @Data
    public static class InsuredResponse {
        private String insuredUuid;
        private String fullName;
        private String membershipNumber;
        private List<DependantResponse> dependants;
    }

    @Data
    public static class DependantResponse {
        private String dependantUuid;
        private String fullName;
        private Relationship relationshipType;
    }
}
