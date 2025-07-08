package com.medco.HealthConnectProvider.ui.response.Payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Setter
@Getter
public class TransferVerification {
    private String message;
    private String status;
    private PaymentVerification.PaymentData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentData {
        private String account_name;
        private String account_number;
        private String mobile;
        private String currency;
        private BigDecimal amount;
        private BigDecimal charge;
        private BigDecimal mode;
        private BigDecimal transfer_method;
        private BigDecimal narration;
        private BigDecimal chapa_transfer_id;
        private BigDecimal bank_code;
        private BigDecimal bank_name;
        private BigDecimal cross_party_reference;
        private BigDecimal ip_address;
        private BigDecimal status;
        private BigDecimal tx_ref;


        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
        private LocalDateTime created_at;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
        private LocalDateTime updated_at;

    }

}
