package com.medco.HealthConnectProvider.ui.response.claims;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProvidedServiceResponse {
    private String patientName;
    private LocalDate dispensingDate;
    private Double totalAmount;
    List<ItemResponse> itemResponses;
}