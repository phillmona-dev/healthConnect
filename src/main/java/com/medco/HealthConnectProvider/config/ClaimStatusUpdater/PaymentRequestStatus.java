package com.medco.HealthConnectProvider.config.ClaimStatusUpdater;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.claims.ClaimLogs;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.claims.ClaimLogsRepository;
import com.medco.HealthConnectProvider.repository.claims.ClaimRepository;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class PaymentRequestStatus implements UpdateClaimStatus {

    private final ClaimRepository claimRepository;
    private final ClaimLogsRepository claimLogsRepository;

    public PaymentRequestStatus(ClaimRepository claimRepository, ClaimLogsRepository claimLogsRepository) {
        this.claimRepository = claimRepository;
        this.claimLogsRepository = claimLogsRepository;
    }

    @Override
    public ResponseEntity<?> updateTransferStatus(String claimUuid, String comment) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();

//        // Check if user has access to this claim
//        if (userDetails.getProviderUuid() == null || !claim.getProviderUuid().equals(userDetails.getPayerUuid())) {
//            throw new BadRequestException("Only provider users can request payment for claims");
//        }

        // Check if claim is in approved status
        if (!claim.getStatus().equals(ClaimStatus.APPROVED)) {
            throw new BadRequestException("Only approved claims can be submitted for payment");
        }

        // Update claim status
        ClaimStatus previousStatus = claim.getStatus();
        claim.setStatus(ClaimStatus.PAYMENT_REQUESTED);
        claim.setPaymentRequestedDate(LocalDateTime.now());
        claim.setPaymentRequestedByUuid(userDetails.getUserUuid());

        claimRepository.save(claim);

        // Create log entry
        createClaimLog(claim, userDetails, previousStatus, ClaimStatus.PAYMENT_REQUESTED,
                "Payment requested by provider");

        return ResponseEntity.ok(new MessageResponse("Payment request submitted successfully"));
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
