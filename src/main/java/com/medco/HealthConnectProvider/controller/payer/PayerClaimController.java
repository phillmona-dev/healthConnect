package com.medco.HealthConnectProvider.controller.payer;

import com.medco.HealthConnectProvider.services.payer.PayerService;
import com.medco.HealthConnectProvider.ui.request.claims.ClaimReviewRequest;
import com.medco.HealthConnectProvider.ui.response.claims.ClaimResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/healthConnect/payer/claims")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payer Claim Management", description = "APIs for payer claim operations")
public class PayerClaimController {

    private final PayerService payerService;

    @Autowired
    public PayerClaimController(PayerService payerService) {
        this.payerService = payerService;
    }

    @GetMapping
    @Operation(summary = "Get claims for payer review", description = "Retrieves claims submitted for payer review")
    public ResponseEntity<Page<ClaimResponse>> getClaimsForReview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(payerService.getClaimsForReview(page, size));
    }

    @PostMapping("/{claimUuid}/review")
    @Operation(summary = "Review claim", description = "Approve or reject a claim")
    public ResponseEntity<?> reviewClaim(
            @PathVariable String claimUuid,
            @RequestBody ClaimReviewRequest reviewRequest) {
        return payerService.reviewClaim(claimUuid, reviewRequest);
    }
}
