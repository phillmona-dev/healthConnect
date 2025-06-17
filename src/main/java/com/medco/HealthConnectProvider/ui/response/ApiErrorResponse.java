package com.medco.HealthConnectProvider.ui.response;

public class ApiErrorResponse {
    private String message;

    public ApiErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
