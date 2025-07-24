package com.medco.HealthConnectProvider.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchRecordDTO {

    private Long id;
    private String batchCode;
    private String payerName;
    private LocalDate requestedOn;
    private LocalDate claimDatingFrom;
    private LocalDate claimDatingTo;
    private Double totalAmount;
    private String status;
    private String claimUuid;

    private double numberOfClaims;
    private String requestedByUserName;

    public String getFormattedTotalAmount() {
        return String.format("%.2f", totalAmount);
    }

    public String getDateRange() {
        return claimDatingFrom + " to " + claimDatingTo;
    }

    public void setClaimData(String claimUuid, int numberOfClaims) {
        this.claimUuid = claimUuid;
        this.numberOfClaims = numberOfClaims;
    }
}
