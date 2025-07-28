package com.medco.HealthConnectProvider.ui.request.contract;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ContractFilterRequest {
    private String contractNumber;
    private String contractName;
    private Status status;
    private String payerUuid;
    private String providerUuid;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDateTo;

    private String preparedBy;
    private Boolean isDeleted;
}
