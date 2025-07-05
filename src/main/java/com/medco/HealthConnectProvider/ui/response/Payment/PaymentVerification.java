package com.medco.HealthConnectProvider.ui.response.Payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentVerification {
    private String message;
    private String status;
    private PaymentData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentData {
        private String first_name;
        private String last_name;
        private String email;
        private String currency;
        private BigDecimal amount;  // Changed from String to BigDecimal
        private BigDecimal charge; // Changed from String to BigDecimal
        private String status;
        private String mode;
        private String method;
        private String reference;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
        private LocalDateTime created_at;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
        private LocalDateTime updated_at;

        private String type;
        private String tx_ref;
        private Customization customization;
        private Object meta; // Can be more specific if you know the structure
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Customization {
        private String title;
        private String description;
        private String logo;
    }
}