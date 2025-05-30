package com.medco.HealthConnectProvider.services.mail;

public interface EmailService {
    void sendWelcomeEmail(String to, String firstName, String password, String payerName, String loginUrl);
}
