package com.medco.HealthConnectProvider.ui.response.groups;

import com.medco.HealthConnectProvider.utils.enums.GroupType;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeInsuredGroupResponse {
    
    private Long id;
    private String insuredUuid;
    private String groupUuid;
    
    // Insured information
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String insuranceId;
    private String phone;
    private String gender;
    private Status status;
    
    // Group information
    private String groupName;
    private String groupDescription;
    private GroupType groupType;
}