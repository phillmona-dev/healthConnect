package com.medco.HealthConnectProvider.ui.response.claims;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClaimStatistics {
    private long pendingClaims;
    private long approvedClaims;
    private long rejectedClaims;
    private long underReviewClaims;
}