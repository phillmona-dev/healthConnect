package com.medco.HealthConnectProvider.ui.request.auth.password.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ContractServiceGroupAssignmentRequest {

    @NotBlank(message = "Contract detail UUID is required")
    private String contractDetailUuid;

    @NotEmpty(message = "At least one employee group UUID is required")
    private List<String> employeeGroupUuids;
}
