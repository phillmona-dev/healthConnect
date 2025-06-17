package com.medco.HealthConnectProvider.ui.request.integration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DispensingItemRequest {

    private String serviceUuid;
    private int quantity;
}
