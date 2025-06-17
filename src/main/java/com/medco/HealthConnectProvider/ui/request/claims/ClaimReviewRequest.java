package com.medco.HealthConnectProvider.ui.request.claims;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClaimReviewRequest {
    private boolean approved;
    private String comment;
}
