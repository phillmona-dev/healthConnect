package com.medco.HealthConnectProvider.ui.response.claims;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDetailResponse {
    // Basic claim information
    private String claimUuid;
    private Long claimNumber;
    private String status;

    // Contract information
    private String contractUuid;
    private String contractCode;
    private String contractName;

    // Provider information
    private String providerUuid;
    private String providerName;
    private String providerCode;

    // Payer information
    private String payerUuid;
    private String payerName;
    private String payerCode;



    // Claim details
    private String mrnNumber;
    private Date visitDate;
    private Double totalAmount;
    private String providerComment;

    // Submission information
    private Date submissionDate;
    private String submittedByUuid;
    private String submittedByName;

    // Provider approval information
    private String approvedByProviderUuid;
    private String approvedByProviderName;
    private String approvedByProviderStatus;
    private Date approvedByProviderDate;

    // Payer approval information
    private String approvedByPayerUuid;
    private String approvedByPayerName;
    private String approvedByPayerStatus;
    private Date approvedByPayerDate;

    // Payment information
    private String paymentRequestedByUuid;
    private Date paymentRequestedDate;
    private String paidByPayerUuid;
    private String paidByPayerName;
    private Date paidDate;
    private String paidStatus;
    private String paymentCode;
    private String paymentType;
    private String checkNumber;
    private String fromBank;
    private String toBank;
    private String transactionNumber;

    // Cancellation information
    private String cancelledByUuid;
    private Date cancelledDate;

    // Related data
    private List<ClaimAttachmentResponse> attachments;
    private List<ClaimCommentResponse> comments;
    private List<ClaimLogResponse> logs;
    private List<ProvidedServiceResponse> services;
//    private ProvidedServiceResponse service;

    

}