package com.medco.HealthConnectProvider.config.ClaimStatusUpdater;

public interface UpdateClaimStatus {
    void updateTransferStatus(String claimUuid,String comment );
}
