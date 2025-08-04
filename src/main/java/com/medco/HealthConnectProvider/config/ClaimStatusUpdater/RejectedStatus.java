package com.medco.HealthConnectProvider.config.ClaimStatusUpdater;

import com.medco.HealthConnectProvider.entity.claims.BatchRecord;
import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.claims.ClaimLogs;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.claims.BatchRecordRepository;
import com.medco.HealthConnectProvider.repository.claims.ClaimRepository;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RejectedStatus implements UpdateClaimStatus {

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private BatchRecordRepository batchRecordRepository;

    @Override
    @Transactional
    public ResponseEntity<?> updateTransferStatus(String claimUuid, String comment) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        claim.setStatus(ClaimStatus.REJECTED);
        claim.addLog(new ClaimLogs(claim, ClaimStatus.REJECTED, comment));
        claimRepository.save(claim);

        updateBatchStatus(claim.getBatchRecord());

        return ResponseEntity.ok(new MessageResponse("Claim rejected successfully"));
    }

    private void updateBatchStatus(BatchRecord batchRecord) {
        if (batchRecord != null) {
            boolean isRejected = false;

            // Check the status of the associated claim
            if (batchRecord.getClaim() != null && batchRecord.getClaim().getStatus() == ClaimStatus.REJECTED) {
                isRejected = true;
            }

            // If not already rejected, check the status of associated medication dispensings
            if (!isRejected && batchRecord.getMedicationDispensing() != null) {
                isRejected = batchRecord.getMedicationDispensing().stream()
                        .anyMatch(md -> "REJECTED".equalsIgnoreCase(md.getClaimStatus()));
            }

            batchRecord.setStatus(isRejected ? "REJECTED" : "APPROVED");
            batchRecordRepository.save(batchRecord);
        }
    }
}
