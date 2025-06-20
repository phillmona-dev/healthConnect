package com.medco.HealthConnectProvider.ui.request.claims;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BatchRecordSearchCriteria {
    private String batchCode;
    private String payerName;
    private LocalDateTime requestedOnStart;
    private LocalDateTime requestedOnEnd;
    private LocalDate claimDatingFrom;
    private LocalDate claimDatingTo;
    private Double minTotalAmount;
    private Double maxTotalAmount;
    private String status;
    private String claimUuid;
}
