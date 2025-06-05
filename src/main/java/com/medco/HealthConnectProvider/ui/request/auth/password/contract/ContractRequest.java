package com.medco.HealthConnectProvider.ui.request.auth.password.contract;

import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ContractRequest {

    @Size(min = 36, max = 40)
    private String providerUuid;

    @NotBlank
    @Size(min = 3, max = 100)
    private String contractName;

    @NotBlank
    @Size(min = 3, max = 100)
    private String contractCode;

    @NotBlank
    @Size(min = 3, max = 200)
    private String description;

    @Size(min = 36, max = 40)
    private String payerUuid;

    private Status status;

    @NotNull
    private Date beginDate;
    @NotNull
    private Date endDate;


}
