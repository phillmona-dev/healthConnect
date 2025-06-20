package com.medco.HealthConnectProvider.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchRecordDTO {

    private Long id;
    private String batchCode;
    private String payerName;
    private LocalDateTime requestedOn;
    private LocalDate claimDatingFrom;
    private LocalDate claimDatingTo;
    private Double totalAmount;
    private String status;
    private String claimUuid; // This will hold the UUID of the associated claim

    // You can add additional fields that might be useful for the client
    private int numberOfClaims; // If a batch can have multiple claims
    private String requestedByUserName; // The name of the user who requested the batch

    // You might want to add some derived fields or formatted strings
    public String getFormattedTotalAmount() {
        return String.format("%.2f", totalAmount);
    }

    public String getDateRange() {
        return claimDatingFrom + " to " + claimDatingTo;
    }

    // If you need to perform any custom mapping or data manipulation,
    // you can add methods here to do so
    public void setClaimData(String claimUuid, int numberOfClaims) {
        this.claimUuid = claimUuid;
        this.numberOfClaims = numberOfClaims;
    }
}
