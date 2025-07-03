package com.medco.HealthConnectProvider.ui.response.claims;

import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
public class ClaimCustomResponse {
    private String claimUuid;
    private String batchCode;
    private LocalDate claimDatingFrom;
    private LocalDate claimDatingTo;
    //    private String dispensingUuid;
    private String payerUuid;

    private String providerUuid;
    //    private String providerName;
//    private String contractUuid;
//    private String contractCode;
    private String mrnNumber;
    private Long claimNumber;
    private LocalDateTime visitDate;
    private BigDecimal totalAmount;
    private ClaimStatus status;
    private LocalDateTime submissionDate;
    private int totalAttachments;
    private int totalComments;
    private int totalClaims;
//    private String payerName;
}
