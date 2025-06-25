package com.medco.HealthConnectProvider.ui.response.groups;

import com.medco.HealthConnectProvider.ui.response.persons.DependantResponse;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredResponse;
import com.medco.HealthConnectProvider.ui.response.service.ServiceResponse;
import com.medco.HealthConnectProvider.utils.enums.GroupType;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class GroupMembersAndServicesResponse {
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

    List<InsuredResponse> insuredResponses;
    List<DependantResponse> dependantResponses;



}
