package com.medco.HealthConnectProvider.services.payment;

import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.payment.PaymentTransaction;

import java.math.BigDecimal;

public interface PaymentService {
    PaymentTransaction initiatePayment(String claimUuid, BigDecimal amount, String currency);
    boolean verifyPayment(String transactionId);
    PaymentTransaction getPaymentStatus(String transactionId);
}
