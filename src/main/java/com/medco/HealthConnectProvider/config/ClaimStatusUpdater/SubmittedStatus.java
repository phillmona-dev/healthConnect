package com.medco.HealthConnectProvider.config.ClaimStatusUpdater;

import org.springframework.stereotype.Component;

@Component
public class SubmittedStatus implements UpdateClaimStatus {

    @Override
    public void updateTransferStatus(String claimUuid,String comment) {

    }
}
