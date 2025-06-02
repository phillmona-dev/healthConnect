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
    
    // Claim submission
    ResponseEntity<?> submitClaim(ClaimRequest claimRequest);
    
    // Claim retrieval
    ClaimDetailResponse getClaimByUuid(String claimUuid);
    List<ClaimResponse> getClaimsByProvider(String providerUuid, Pageable pageable);
    List<ClaimResponse> getClaimsByPayer(String payerUuid, Pageable pageable);
    List<ClaimResponse> getClaimsByStatus(ClaimStatus status, Pageable pageable);
    
    // Claim processing
    ResponseEntity<?> updateClaimStatus(String claimUuid, ClaimStatus newStatus, String comment);
    ResponseEntity<?> reviewClaim(String claimUuid, boolean approved, String reviewComment);
    
    // Claim attachments
    ResponseEntity<?> addClaimAttachment(String claimUuid, MultipartFile file);
    ResponseEntity<?> deleteClaimAttachment(String attachmentUuid);
    
    // Claim comments
    ResponseEntity<?> addClaimComment(String claimUuid, ClaimCommentRequest commentRequest);
    
    // Claim payments
    ResponseEntity<?> requestPayment(String claimUuid);
    ResponseEntity<?> processPayment(String claimUuid, ClaimPaymentRequest paymentRequest);
    
    // Claim logs
    List<?> getClaimLogs(String claimUuid, Pageable pageable);
}