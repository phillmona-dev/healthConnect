package com.medco.HealthConnectProvider.config.ClaimStatusUpdater;

import org.springframework.http.ResponseEntity;

public interface UpdateClaimStatus {
    ResponseEntity<?> updateTransferStatus(String claimUuid, String comment );
}
