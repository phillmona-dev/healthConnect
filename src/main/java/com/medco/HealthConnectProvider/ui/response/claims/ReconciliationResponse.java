package com.medco.HealthConnectProvider.ui.response.claims;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReconciliationResponse {
    private String batchCode;
    private String payerName;
    private LocalDate requestedOn;
    private LocalDate claimDatingFrom;
    private LocalDate claimDatingTo;
    private BigDecimal totalAmount;
    private String status;
    private String message;
}
