package com.medco.HealthConnectProvider.ui.request.group;

import com.medco.HealthConnectProvider.utils.enums.GroupType;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeDependantGroupRequest {
    
    @NotBlank(message = "Group name is required")
    @Size(min = 3, max = 100, message = "Group name must be between 3 and 100 characters")
    private String groupName;
    
    @Size(max = 500, message = "Group description cannot exceed 500 characters")
    private String groupDescription;
    
    private Integer estimatedMembers;
    
    private GroupType type;
    
    private Status status;
}