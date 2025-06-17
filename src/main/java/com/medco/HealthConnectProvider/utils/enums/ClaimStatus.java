package com.medco.HealthConnectProvider.utils.enums;

public enum ClaimStatus {
    DRAFT,
    SUBMITTED,           // Initial state when claim is submitted
    UNDER_REVIEW,        // Claim is under review by payer
    APPROVED,            // Claim has been approved by payer
    REJECTED,            // Claim has been rejected
    PAYMENT_REQUESTED,   // Provider has requested payment for approved claim
    PAID,                // Payment has been processed
    CANCELLED,            // Claim has been cancelled
    PAYMENT_INITIATED,
    RECONCILED
}