package com.medco.HealthConnectProvider.ui.response.Payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.reactive.function.client.ClientResponse;


@Setter
@Getter
public class PaymentResponse {
    private String ClientUuid;
    private String ClientName;
    private String  gender;
    private String phoneNumber;
    private String email;
    private String insuranceName;
    private boolean isInsured;
    private String paymentUuid;
    private String paymentType;
    private String paymentDate;
    private String paymentDescription;
    private String paymentStatus;
    private double amount;
    private String currency="ETB";
    private ClientResponse clientResponse;
//    private List<ServiceResponse> patientServices;

}

