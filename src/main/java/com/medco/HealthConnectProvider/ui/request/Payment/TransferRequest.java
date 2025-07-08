package com.medco.HealthConnectProvider.ui.request.Payment;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TransferRequest {

    private String account_name;
    private String account_number;
    private double amount;
    private String currency;
    private String reference;
    private String bank_code;

}
