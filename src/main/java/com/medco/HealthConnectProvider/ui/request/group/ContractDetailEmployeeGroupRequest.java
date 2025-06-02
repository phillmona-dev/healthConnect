package com.medco.HealthConnectProvider.ui.request.group;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractDetailEmployeeGroupRequest {
    
    @NotBlank(message = "Contract detail UUID is required")
    private String contractDetailUuid;
    
    @NotBlank(message = "Employee group UUID is required")
    private String employeeGroupUuid;
}