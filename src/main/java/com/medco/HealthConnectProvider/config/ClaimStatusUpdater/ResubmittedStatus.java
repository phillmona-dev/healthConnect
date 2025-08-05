package com.medco.HealthConnectProvider.config.ClaimStatusUpdater;

import com.medco.HealthConnectProvider.entity.claims.BatchRecord;
import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.claims.ClaimLogs;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensing;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.claims.BatchRecordRepository;
import com.medco.HealthConnectProvider.repository.claims.ClaimRepository;
import com.medco.HealthConnectProvider.repository.integration.MedicationDispensingRepository;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ResubmittedStatus implements UpdateClaimStatus {

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private BatchRecordRepository batchRecordRepository;

    @Autowired
    private MedicationDispensingRepository medicationDispensingRepository;

    @Override
    @Transactional
    public ResponseEntity<?> updateTransferStatus(String claimUuid, String comment) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        List<MedicationDispensing> dispensings = medicationDispensingRepository.findByClaimUuid(claimUuid);

        boolean hasResubmittedItems = dispensings.stream()
                .anyMatch(md -> "RESUBMITTED".equalsIgnoreCase(md.getClaimStatus()));

        boolean hasRejectedItems = dispensings.stream()
                .anyMatch(md -> "REJECTED".equalsIgnoreCase(md.getClaimStatus()));

        if (hasResubmittedItems && !hasRejectedItems) {
            claim.setStatus(ClaimStatus.RESUBMITTED);
            claim.addLog(new ClaimLogs(claim, ClaimStatus.RESUBMITTED, "Auto-resubmitted: " + comment));
            claimRepository.save(claim);

            dispensings.forEach(md -> {
                if (!"RESUBMITTED".equalsIgnoreCase(md.getClaimStatus())) {
                    md.setClaimStatus("RESUBMITTED");
                    medicationDispensingRepository.save(md);
                }
            });

            updateBatchStatus(claim.getBatchRecord());
            return ResponseEntity.ok(new MessageResponse("Claim and all items resubmitted successfully"));
        }

        if (claim.getStatus() != ClaimStatus.REJECTED) {
            throw new BadRequestException("Only rejected claims or claims with resubmitted items can be resubmitted");
        }

        claim.setStatus(ClaimStatus.RESUBMITTED);
        claim.addLog(new ClaimLogs(claim, ClaimStatus.RESUBMITTED, comment));
        claimRepository.save(claim);

        updateBatchStatus(claim.getBatchRecord());

        return ResponseEntity.ok(new MessageResponse("Claim resubmitted successfully"));
    }

    private void updateBatchStatus(BatchRecord batchRecord) {
        if (batchRecord != null) {
            boolean hasRejected = false;
            boolean hasResubmitted = false;

            if (batchRecord.getClaim() != null) {
                if (batchRecord.getClaim().getStatus() == ClaimStatus.REJECTED) {
                    hasRejected = true;
                } else if (batchRecord.getClaim().getStatus() == ClaimStatus.RESUBMITTED) {
                    hasResubmitted = true;
                }
            }

            if (!hasRejected && batchRecord.getMedicationDispensing() != null) {
                hasRejected = batchRecord.getMedicationDispensing().stream()
                        .anyMatch(md -> "REJECTED".equalsIgnoreCase(md.getClaimStatus()));

                hasResubmitted = batchRecord.getMedicationDispensing().stream()
                        .anyMatch(md -> "RESUBMITTED".equalsIgnoreCase(md.getClaimStatus()));
            }

            if (hasRejected) {
                batchRecord.setStatus("REJECTED");
            } else if (hasResubmitted) {
                batchRecord.setStatus("RESUBMITTED");
            } else {
                batchRecord.setStatus("APPROVED");
            }

            batchRecordRepository.save(batchRecord);
        }
    }
}