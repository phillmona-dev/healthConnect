package com.medco.HealthConnectProvider.ui.response.claims;

import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClaimCustomResponse {
    private String claimUuid;
    private String batchCode;
    private LocalDate claimDatingFrom;
    private LocalDate claimDatingTo;
    private String payerUuid;
    private String providerUuid;
    private String mrnNumber;
    private Long claimNumber;
    private LocalDateTime visitDate;
    private BigDecimal totalAmount;
    private ClaimStatus status;
    private LocalDateTime submissionDate;
    private int totalAttachments;
    private int totalComments;
    private int totalClaims;

    public ClaimCustomResponse(String claimUuid, String batchCode, LocalDate claimDatingFrom,
                               LocalDate claimDatingTo, String payerUuid, String providerUuid,
                               String mrnNumber, Long claimNumber, LocalDateTime visitDate,
                               BigDecimal totalAmount, ClaimStatus status, LocalDateTime submissionDate,
                               Long totalAttachments, Long totalComments, Long totalClaims) {
        this.claimUuid = claimUuid;
        this.batchCode = batchCode;
        this.claimDatingFrom = claimDatingFrom;
        this.claimDatingTo = claimDatingTo;
        this.payerUuid = payerUuid;
        this.providerUuid = providerUuid;
        this.mrnNumber = mrnNumber;
        this.claimNumber = claimNumber;
        this.visitDate = visitDate;
        this.totalAmount = totalAmount;
        this.status = status;
        this.submissionDate = submissionDate;
        this.totalAttachments = totalAttachments.intValue();
        this.totalComments = totalComments.intValue();
        this.totalClaims = totalClaims.intValue();
    }
}
