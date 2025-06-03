package com.medco.HealthConnectProvider.ui.request.auth.password.payer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayerWithBase64LogoRequest {
    private PayerRequest payer;
    private String base64Logo;
}