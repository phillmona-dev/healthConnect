package com.medco.HealthConnectProvider.ui.response.groups;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Response DTO for employee groups
 * Used to represent employee/dependant groups in API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeGroupResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String groupUuid;
    private String groupName;
    private String groupDescription;
    private Integer estimatedMembers;
    private String payerUuid;
    private String payerName;
    private String status;
    private String groupType;
}