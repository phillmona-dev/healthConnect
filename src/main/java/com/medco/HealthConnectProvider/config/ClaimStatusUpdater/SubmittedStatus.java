package com.medco.HealthConnectProvider.config.ClaimStatusUpdater;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.claims.ClaimLogs;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensing;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.claims.ClaimLogsRepository;
import com.medco.HealthConnectProvider.repository.claims.ClaimRepository;
import com.medco.HealthConnectProvider.repository.integration.MedicationDispensingRepository;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SubmittedStatus implements UpdateClaimStatus {

    private final ClaimRepository claimRepository;
    private final ClaimLogsRepository  claimLogsRepository;
    private final MedicationDispensingRepository  medicationDispensingRepository;

    public SubmittedStatus(ClaimRepository claimRepository, ClaimLogsRepository claimLogsRepository, MedicationDispensingRepository medicationDispensingRepository) {
        this.claimRepository = claimRepository;
        this.claimLogsRepository = claimLogsRepository;
        this.medicationDispensingRepository = medicationDispensingRepository;
    }

    @Override
    public ResponseEntity<?> updateTransferStatus(String claimUuid,String comment) {

        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        ClaimStatus previousStatus = claim.getStatus();

        // Validate status transition
        validateStatusTransition(claim, ClaimStatus.SUBMITTED);

        // Update claim status
        claim.setStatus(ClaimStatus.SUBMITTED);

        // Update specific status fields based on the new status
        updateStatusSpecificFields(claim, ClaimStatus.SUBMITTED, userDetails);
        List<MedicationDispensing> medicationDispensingList=new ArrayList<>();
        for (MedicationDispensing medicationDispensing:claim.getBatchRecord().getMedicationDispensing()){
            medicationDispensing.setClaimStatus(ClaimStatus.SUBMITTED.toString());
            medicationDispensingList.add(medicationDispensing);
        }
        medicationDispensingRepository.saveAll(medicationDispensingList);

        // Save updated claim
        claimRepository.save(claim);

        // Create claim log
        createClaimLog(claim, userDetails, previousStatus, ClaimStatus.SUBMITTED, comment);

        return ResponseEntity.ok(new MessageResponse("Claim status updated successfully to " + ClaimStatus.SUBMITTED));
//        return ResponseEntity.ok("you have "+newStatus+" the the claim.");

    }
    // Helper methods for claim processing
    private void validateStatusTransition(Claim claim, ClaimStatus newStatus) {
        String currentStatus = String.valueOf(claim.getStatus());


        // Define valid transitions
        if (currentStatus.equals(ClaimStatus.SUBMITTED.toString())) {
            if (newStatus != ClaimStatus.UNDER_REVIEW && newStatus != ClaimStatus.REJECTED && newStatus != ClaimStatus.CANCELLED) {
                throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
            }
        } else if (currentStatus.equals(ClaimStatus.UNDER_REVIEW.toString())) {
            if (newStatus != ClaimStatus.APPROVED && newStatus != ClaimStatus.REJECTED && newStatus != ClaimStatus.CANCELLED) {
                throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
            }
        } else if (currentStatus.equals(ClaimStatus.APPROVED.toString())) {
            if (newStatus != ClaimStatus.PAYMENT_REQUESTED && newStatus != ClaimStatus.CANCELLED) {
                throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
            }
        } else if (currentStatus.equals(ClaimStatus.PAYMENT_REQUESTED.toString())) {
            if (newStatus != ClaimStatus.PAID && newStatus != ClaimStatus.CANCELLED) {
                throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
            }
        } else if (currentStatus.equals(ClaimStatus.PAID.toString()) ||
                currentStatus.equals(ClaimStatus.REJECTED.toString()) ||
                currentStatus.equals(ClaimStatus.CANCELLED.toString())) {
            throw new BadRequestException("Cannot change status of a claim that is " + currentStatus);
        }
    }

    private void updateStatusSpecificFields(Claim claim, ClaimStatus newStatus, UserPrincipal userDetails) {
        switch (newStatus) {
            case SUBMITTED:
                claim.setPreparedByProviderUuid(userDetails.getUserUuid());
                claim.setPreparedByProviderStatus("Submitted");
                claim.setPreparedByProviderDate(LocalDateTime.now());
                break;
            case UNDER_REVIEW:
                // Already handled in reviewClaim method
                break;
            case APPROVED:
                // Already handled in reviewClaim method
                break;
            case PAYMENT_REQUESTED:
                claim.setCancelledDate(LocalDateTime.now());
                claim.setPaymentRequestedByUuid(userDetails.getUserUuid());
                break;
            case PAID:
                // Handled in processPayment method
                break;
            case REJECTED:
                // Already handled in reviewClaim method
                break;
            case CANCELLED:
                claim.setCancelledDate(LocalDateTime.from(Instant.now()));
                claim.setCancelledByUuid(userDetails.getUserUuid());
                break;
            default:
                break;
        }
    }

    private void createClaimLog(Claim claim, UserPrincipal userDetails, ClaimStatus previousStatus, ClaimStatus newStatus, String comment) {
        ClaimLogs log = new ClaimLogs();
        log.setLogUuid(UUID.randomUUID().toString());
        log.setClaimUuid(claim.getClaimUuid());
        log.setClaim(claim);
        log.setActionByUuid(userDetails.getUserUuid());
        log.setActionByName(userDetails.getFirstName() + " " + userDetails.getFatherName());
        log.setActionByRole(userDetails.getAuthorities().iterator().next().getAuthority());
        log.setActionDate(Instant.now());
        log.setPreviousStatus(String.valueOf(previousStatus));
        log.setActionStatus(String.valueOf(newStatus));
        log.setComment(comment);

        claimLogsRepository.save(log);
    }
}
