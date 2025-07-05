package com.medco.HealthConnectProvider.exception;

public class BankApiException extends RuntimeException {
    public BankApiException(String message) {
        super(message);
    }
}
