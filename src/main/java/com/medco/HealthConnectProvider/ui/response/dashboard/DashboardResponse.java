package com.medco.HealthConnectProvider.ui.response.dashboard;

import com.medco.HealthConnectProvider.ui.response.claims.ClaimStatistics;
import com.medco.HealthConnectProvider.ui.response.payer.PayerSummary;
import com.medco.HealthConnectProvider.ui.response.provider.ProviderSummary;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponse {

    private long totalPayers;
    private long totalProviders;
    private long totalInsured;
    private long totalClaims;
    private long totalGroups;
    private List<PayerSummary> payerSummaries;
    private List<ProviderSummary> providerSummaries;
    private ClaimStatistics claimStatistics;
    private Map<String, Integer> monthlyClaimTotals;

}