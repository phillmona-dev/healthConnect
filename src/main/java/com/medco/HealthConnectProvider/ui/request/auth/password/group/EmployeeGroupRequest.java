package com.medco.HealthConnectProvider.ui.request.auth.password.group;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeGroupRequest {

    @NotBlank(message = "Group name is required")
    private String groupName;
    private String groupDescription;
    private Integer estimatedMembers;
}
