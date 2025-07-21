package com.medco.HealthConnectProvider.ui.response.dashboard;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ComprehensiveDashboardResponse {

    private int totalPayers;
    private int totalProviders;
    private int totalInsured;
    private int totalClaims;
    private List<PayerStatistics> payerStatistics;
    private Map<String, Integer> monthlyClaimRequests;

    @Data
    public static class PayerStatistics {
        private String payerUuid;
        private String payerName;
        private int totalGroups;
        private int totalInsured;
        private List<GroupStatistics> groupStatistics;
    }

    @Data
    public static class GroupStatistics {
        private String groupUuid;
        private String groupName;
        private int insuredCount;
    }
}
