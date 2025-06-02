package com.medco.HealthConnectProvider.ui.response.groups;

import com.medco.HealthConnectProvider.utils.enums.GroupType;
import com.medco.HealthConnectProvider.utils.enums.Relationship;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DependantGroupResponse {
    
    private Long id;
    private String dependantUuid;
    private String groupUuid;
    
    // Dependant information
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private Relationship relationship;
    private String gender;
    private Status status;
    
    // Insured information (parent of dependant)
    private String insuredUuid;
    private String insuredName;
    private String insuranceId;
    
    // Group information
    private String groupName;
    private String groupDescription;
    private GroupType groupType;
}