package com.medco.HealthConnectProvider.ui.response.contracts;

import lombok.Data;
import java.util.List;

@Data
public class AddInsuredToContractResponse {
    private String message;
    private String contractUuid;
    private List<InsuredResponse> addedInsured;
    private List<DependantResponse> addedDependants;

    @Data
    public static class InsuredResponse {
        private String insuredUuid;
        private String fullName;
        private String membershipNumber;
    }

    @Data
    public static class DependantResponse {
        private String dependantUuid;
        private String fullName;
        private String relationshipType;
    }
}
