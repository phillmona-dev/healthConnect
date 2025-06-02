package com.medco.HealthConnectProvider.ui.request.claims;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimPaymentRequest {
    
    @NotNull(message = "Payment amount is required")
    @Positive(message = "Payment amount must be positive")
    private Double amount;
    
    @NotBlank(message = "Payment type is required")
    private String paymentType; // BANK_TRANSFER, CHECK, CASH
    
    @Size(max = 40, message = "Check number cannot exceed 40 characters")
    private String checkNumber; // Required if payment type is CHECK
    
    private String fromBank;
    
    private String toBank;
    
    private String transactionNumber;
}