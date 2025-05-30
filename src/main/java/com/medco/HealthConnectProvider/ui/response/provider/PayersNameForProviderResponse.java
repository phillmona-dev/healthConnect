package com.medco.HealthConnectProvider.ui.response.provider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class PayersNameForProviderResponse implements Serializable {
    @NotBlank
    @Size(min = 36, max = 40)
    private String payerUuid;

    @Size(min = 3, max = 200)
    private String payerName;
    private long totalPages;
}
