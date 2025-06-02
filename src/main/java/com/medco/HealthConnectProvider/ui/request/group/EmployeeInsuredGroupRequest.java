package com.medco.HealthConnectProvider.ui.request.group;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeInsuredGroupRequest {
    
    @NotBlank(message = "Insured UUID is required")
    private String insuredUuid;
    
    @NotBlank(message = "Group UUID is required")
    private String groupUuid;
}