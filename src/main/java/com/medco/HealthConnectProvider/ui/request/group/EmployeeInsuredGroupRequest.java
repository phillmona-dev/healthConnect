package com.medco.HealthConnectProvider.ui.request.group;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeInsuredGroupRequest {

    private List<String> insuredUuids;
    

}