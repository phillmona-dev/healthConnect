package com.medco.HealthConnectProvider.ui.request.auth.password.contract;

import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class ContractRequest {
    @NotBlank
    @Size(min = 36, max = 40)
    private String providerUuid;

    @NotBlank
    @Size(min = 3, max = 200)
    private String description;

    @NotBlank
    @Size(min = 36, max = 40)
    private String payerUuid;

    private Status status;

    @NotNull
    private Date beginDate;

    @NotNull
    private Date endDate;

    private List<ContractItemRequest> contractItems;

    @Getter
    @Setter
    public static class ContractItemRequest {
        private String itemUuid;
        private String serviceName;
        private String itemType;
        private BigDecimal negotiatedPrice;
    }
}
