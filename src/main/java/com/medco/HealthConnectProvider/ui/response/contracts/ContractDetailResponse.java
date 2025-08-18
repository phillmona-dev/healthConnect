package com.medco.HealthConnectProvider.ui.response.contracts;

import com.medco.HealthConnectProvider.ui.response.groups.EmployeeGroupResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractDetailResponse {
    private String contractDetailUuid;
    private String serviceUuid;
    private String serviceName;
    private String serviceCode;
    private String serviceCategory;
    private String serviceSubCategory;
    private Double negotiatedPrice;
    private Double defaultPrice;
    private String status;
    private List<EmployeeGroupResponse> assignedGroups = new ArrayList<>();
}