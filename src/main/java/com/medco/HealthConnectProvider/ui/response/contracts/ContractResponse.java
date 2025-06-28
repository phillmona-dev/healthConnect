package com.medco.HealthConnectProvider.ui.response.contracts;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractResponse {
    private Long id;
    private String contractHeaderUuid;
    private String contractNumber;
    private String contractName;
    private String contractDescription;
    private String approvedBy;
    private Date approvalDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private Status status;
    private String remark;
    private String preparedBy;
    private String description;
    private String contractCode;
    private Date terminationDate;
    private String terminationReason;
    private String terminationNotes;
    private String terminatedBy;
    private Date terminationRequestDate;
    private Double coPaymentPercentage;
    private boolean isDeleted;

    // Related entity information
    private String payerUuid;
    private String payerName;
    private String payerCode;
    private String providerUuid;
    private String providerName;
    private String providerCode;

    private double negotiatingPrice;

    private Instant createdAt;
    private Instant updatedAt;
}
