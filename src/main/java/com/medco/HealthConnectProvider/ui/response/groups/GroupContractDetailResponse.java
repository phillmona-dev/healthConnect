package com.medco.HealthConnectProvider.ui.response.groups;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupContractDetailResponse {
    private String contractDetailUuid;
    private String serviceName;
    private String serviceCode;
    private BigDecimal negotiatedPrice;
    private String status;
}
