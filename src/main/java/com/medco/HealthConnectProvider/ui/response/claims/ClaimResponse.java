package com.medco.HealthConnectProvider.ui.response.claims;

import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClaimResponse implements Serializable {

    private String claimUuid;

    private String insuredPersonUuid;

    private String payerUuid;
    private String mrnNumber;
    private Date visitDate;

    private String serviceProvidedUuid;

    private String preparedByProviderUuid;


    private String checkedByProviderUuid;

    private String auditedByProviderUuid;

    private String approvedByProviderUuid;

    private String auditedByPayerUuid;
    private String approvedByPayerUuid;
    private String paiedByProviderUuid;

    private Date fromDate;
    private Date toDate;

    private String preparedByProviderStatus;
    private String checkedByProviderStatus;

    private String auditedByProviderStatus;

    private String approvedByProviderStatus;

    private String auditedByPayerStatus;
    private String approvedByPayerStatus;


    private Date preparedByProviderDate;
    private Date checkedByProviderDate;
    private Date auditedByProviderDate;
    private Date approvedByProviderDate;


    private Date auditedByPayerDate;
    private Date approvedByPayerDate;
    private Date paidDate;

    private String checkNumber;
    private String bankTransactionCode;

    private String fromBankUuid;
    private String toBankUuid;

    private String uploadReciept;

    long totalPages;


}
