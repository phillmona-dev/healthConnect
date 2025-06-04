package com.medco.HealthConnectProvider.ui.response.groups;

import com.medco.HealthConnectProvider.utils.enums.GroupType;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeDependantGroupResponse {
    
    private Long id;
    private String groupUuid;
    private String groupName;
    private String groupDescription;
    private Integer estimatedMembers;
    private GroupType type;
    private Status status;
    private String payerUuid;
    private String payerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long totalPages;
}