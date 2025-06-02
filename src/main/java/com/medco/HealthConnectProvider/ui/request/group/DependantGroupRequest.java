package com.medco.HealthConnectProvider.ui.request.group;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DependantGroupRequest {
    
    @NotBlank(message = "Dependant UUID is required")
    private String dependantUuid;
    
    @NotBlank(message = "Group UUID is required")
    private String groupUuid;
}