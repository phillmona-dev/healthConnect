package com.medco.HealthConnectProvider.ui.request.Payment;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WebhookPayload {
    private String event;
    private String first_name;
    private String last_name;
    private String email;
    private String mobile;
    private String currency;
    private String amount;
    private String charge;
    private String status;
    private String mode;
    private String reference;
    private String created_at;
    private String updated_at;
    private String type;
    private String tx_ref;
    private String payment_method;
    @Setter
    @Getter
    private static class customization {
        private String title;
        private String description;
        private String logo;
    }
    private String meta;



}

