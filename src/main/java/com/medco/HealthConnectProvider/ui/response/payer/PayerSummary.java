package com.medco.HealthConnectProvider.ui.response.payer;

import com.medco.HealthConnectProvider.ui.response.groups.GroupSummary;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PayerSummary {

    private String payerUuid;
    private String payerName;
    private long totalInsured;
    private long totalClaims;
    private long totalGroups;
    private List<GroupSummary> groupSummaries;

}
