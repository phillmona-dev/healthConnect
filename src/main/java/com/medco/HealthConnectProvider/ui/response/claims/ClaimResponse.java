package com.medco.HealthConnectProvider.ui.response.claims;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponse {
    private String claimUuid;
    private String payerUuid;
    private String payerName;
    private String providerUuid;
    private String providerName;
    private String contractUuid;
    private String contractCode;
    private String insuredPersonUuid;
    private String insuredPersonName;
    private String dependantUuid;
    private String dependantFullName;
    private String mrnNumber;
    private Long claimNumber;
    private Date visitDate;
    private Double totalAmount;
    private String status;
    private Date submissionDate;
    private int totalAttachments;
    private int totalComments;
    private int totalPages;
}
