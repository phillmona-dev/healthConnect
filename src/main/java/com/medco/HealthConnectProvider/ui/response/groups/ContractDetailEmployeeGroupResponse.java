package com.medco.HealthConnectProvider.ui.response.groups;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractDetailEmployeeGroupResponse {
    
    private Long id;
    private String contractDetailUuid;
    private String employeeGroupUuid;
    
    // Contract detail information
    private String serviceName;
    private String serviceCode;
    private BigDecimal servicePrice;
    private String contractName;
    private String contractCode;
    
    // Employee group information
    private String groupName;
    private String groupDescription;
    private Integer estimatedMembers;
}