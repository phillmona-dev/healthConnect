package com.medco.HealthConnectProvider.ui.response.eligibility;

import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupMembershipResponse {
    private String groupUuid;
    private String groupName;
    private String groupDescription;
    private String groupType;
    private Status groupStatus;
}