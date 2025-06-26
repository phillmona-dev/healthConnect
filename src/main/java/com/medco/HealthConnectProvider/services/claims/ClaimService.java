package com.medco.HealthConnectProvider.services.claims;

import com.medco.HealthConnectProvider.ui.request.claims.ClaimRequest;
import com.medco.HealthConnectProvider.ui.request.claims.ClaimCommentRequest;
import com.medco.HealthConnectProvider.ui.request.claims.ClaimPaymentRequest;
import com.medco.HealthConnectProvider.ui.response.claims.ClaimResponse;
import com.medco.HealthConnectProvider.ui.response.claims.ClaimDetailResponse;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ClaimService {

    ResponseEntity<?> submitClaim(ClaimRequest claimRequest);

    ClaimDetailResponse getClaimByUuid(String claimUuid);
    List<ClaimResponse> getClaimsByProvider(String providerUuid, Pageable pageable);
    List<ClaimResponse> getClaimsByPayer(String payerUuid, Pageable pageable);
    List<ClaimResponse> getClaimsByStatus(ClaimStatus status, Pageable pageable);

    ResponseEntity<?> updateClaimStatus(String claimUuid, ClaimStatus newStatus, String comment);
    ResponseEntity<?> reviewClaim(String claimUuid, boolean approved, String reviewComment);

    ResponseEntity<?> addClaimAttachment(String claimUuid, MultipartFile file);
    ResponseEntity<?> deleteClaimAttachment(String attachmentUuid);

    ResponseEntity<?> addClaimComment(String claimUuid, ClaimCommentRequest commentRequest);

    ResponseEntity<?> requestPayment(String claimUuid);
    ResponseEntity<?> processPayment(String claimUuid, ClaimPaymentRequest paymentRequest);

    List<?> getClaimLogs(String claimUuid, Pageable pageable);

    ResponseEntity<?> verifyPayment(String claimUuid);

    ResponseEntity<?> createBatchClaim(String batchCode);

    ClaimResponse getAll(Pageable pageable);
}