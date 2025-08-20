package com.medco.HealthConnectProvider.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimPaySyncRequest {

    private String contractUuid;

    private String providerUuid;

    private Date claimFromDate;

    private Date claimToDate;

    private double totalAmount;

    private String batchCode;

    private List<String> serviceProvidedUuid;
}
