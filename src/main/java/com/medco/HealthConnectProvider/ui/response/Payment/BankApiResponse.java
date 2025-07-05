package com.medco.HealthConnectProvider.ui.response.Payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankApiResponse {
    private String message;
    private List<Bank> data;

    @Data
    public static class Bank {
        private Long id;
        private String slug;
        private String swift;  // This is the bank code
        private String name;
        private Integer acct_length;
        private Integer country_id;
        private Object is_mobilemoney;
        private Integer is_active;
        private Integer is_rtgs;
        private Integer active;
        private Object is_24hrs;
        private String created_at;
        private String updated_at;
        private String currency;
    }
}
