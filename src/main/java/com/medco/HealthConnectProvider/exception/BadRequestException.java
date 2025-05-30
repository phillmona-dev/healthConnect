package com.medco.HealthConnectProvider.exception;

import jakarta.validation.constraints.NotNull;

public class BadRequestException extends RuntimeException{

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String role, String roleUuid, @NotNull(message = "you need to select a role for the user") String roleUuid1) {
    }
}

