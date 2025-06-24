package com.medco.HealthConnectProvider.controller.claims;

import com.medco.HealthConnectProvider.services.claims.ClaimService;
import com.medco.HealthConnectProvider.services.integration.PharmacyIntegrationService;
import com.medco.HealthConnectProvider.ui.request.claims.ClaimCommentRequest;
import com.medco.HealthConnectProvider.ui.request.claims.ClaimPaymentRequest;
import com.medco.HealthConnectProvider.ui.request.claims.ClaimRequest;
import com.medco.HealthConnectProvider.ui.response.claims.ClaimDetailResponse;
import com.medco.HealthConnectProvider.ui.response.claims.ClaimResponse;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/claims")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Claims", description = "Claim management APIs")
public class ClaimController {

    private final ClaimService claimService;


    private final PharmacyIntegrationService pharmacyIntegrationService;

    public ClaimController(ClaimService claimService, PharmacyIntegrationService pharmacyIntegrationService) {
        this.claimService = claimService;
        this.pharmacyIntegrationService = pharmacyIntegrationService;
    }


    @PostMapping
    @Operation(summary = "Submit a new claim")
    public ResponseEntity<?> submitClaim(@Valid @RequestBody ClaimRequest claimRequest) {
        return claimService.submitClaim(claimRequest);
    }

    @GetMapping("/{claimUuid}")
    @Operation(summary = "Get claim details by UUID")
    public ClaimDetailResponse getClaimByUuid(@PathVariable String claimUuid) {
        return claimService.getClaimByUuid(claimUuid);
    }
    
    @GetMapping("/provider/{providerUuid}")
    @Operation(summary = "Get claims by provider")
    public List<ClaimResponse> getClaimsByProvider(
            @PathVariable String providerUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return claimService.getClaimsByProvider(providerUuid, pageable);
    }
    
    @GetMapping("/payer/{payerUuid}")
    @Operation(summary = "Get claims by payer")
    public List<ClaimResponse> getClaimsByPayer(
            @PathVariable String payerUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return claimService.getClaimsByPayer(payerUuid, pageable);
    }


    @GetMapping("/status/{status}")
    @Operation(summary = "Get claims by status")
    public List<ClaimResponse> getClaimsByStatus(
            @PathVariable ClaimStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return claimService.getClaimsByStatus(status, pageable);
    }
    
    @PutMapping("/{claimUuid}/status")
    @Operation(summary = "Update claim status")
    public ResponseEntity<?> updateClaimStatus(
            @PathVariable String claimUuid,
            @RequestParam ClaimStatus newStatus,
            @RequestParam(required = false) String comment) {
        return claimService.updateClaimStatus(claimUuid, newStatus, comment);
    }
    
    @PutMapping("/{claimUuid}/review")
    @Operation(summary = "Review a claim (approve or reject)")
    public ResponseEntity<?> reviewClaim(
            @PathVariable String claimUuid,
            @RequestParam boolean approved,
            @RequestParam(required = false) String reviewComment) {
        return claimService.reviewClaim(claimUuid, approved, reviewComment);
    }
    
    @PostMapping(value = "/{claimUuid}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add attachment to a claim")
    public ResponseEntity<?> addClaimAttachment(
            @PathVariable String claimUuid,
            @RequestParam("file") MultipartFile file) {
        return claimService.addClaimAttachment(claimUuid, file);
    }
    
    @DeleteMapping("/attachments/{attachmentUuid}")
    @Operation(summary = "Delete claim attachment")
    public ResponseEntity<?> deleteClaimAttachment(@PathVariable String attachmentUuid) {
        return claimService.deleteClaimAttachment(attachmentUuid);
    }
    
    @PostMapping("/{claimUuid}/comments")
    @Operation(summary = "Add comment to a claim")
    public ResponseEntity<?> addClaimComment(
            @PathVariable String claimUuid,
            @Valid @RequestBody ClaimCommentRequest commentRequest) {
        return claimService.addClaimComment(claimUuid, commentRequest);
    }

    @PostMapping("/{claimUuid}/payment/request")
    @Operation(summary = "Request payment for an approved claim")
    public ResponseEntity<?> requestPayment(@PathVariable String claimUuid) {
        return claimService.requestPayment(claimUuid);
    }

    @PostMapping("/{claimUuid}/payment/process")
    @Operation(summary = "Process payment for a claim with payment requested")
    public ResponseEntity<?> processPayment(
            @PathVariable String claimUuid,
            @Valid @RequestBody ClaimPaymentRequest paymentRequest) {
        return claimService.processPayment(claimUuid, paymentRequest);
    }

    @GetMapping("/{claimUuid}/logs")
    @Operation(summary = "Get claim logs")
    public List<?> getClaimLogs(
            @PathVariable String claimUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("actionDate").descending());
        return claimService.getClaimLogs(claimUuid, pageable);
    }

    //payment

    @PostMapping("/claims/{claimUuid}/payment")
    public ResponseEntity<?> initiatePayment(@PathVariable String claimUuid, @RequestBody ClaimPaymentRequest paymentRequest) {
        return claimService.processPayment(claimUuid, paymentRequest);
    }

    @PostMapping("/claims/{claimUuid}/payment/verify")
    public ResponseEntity<?> verifyPayment(@PathVariable String claimUuid) {
        return claimService.verifyPayment(claimUuid);
    }

    @PostMapping("/claims/{claimUuid}/reconcile")
    public ResponseEntity<?> reconcilePayment(@PathVariable String claimUuid) {
        return pharmacyIntegrationService.reconcilePayment(claimUuid);
    }
}